import {
  existsSync,
  readFileSync,
  readdirSync,
  statSync,
} from 'node:fs';

import path from 'node:path';
import { fileURLToPath } from 'node:url';

const SCRIPT_DIRECTORY = path.dirname(fileURLToPath(import.meta.url));
const REPOSITORY_ROOT = path.resolve(SCRIPT_DIRECTORY, '..');

const WEB_ROOT = path.join(REPOSITORY_ROOT, 'apps', 'web');
const APP_ROOT = path.join(WEB_ROOT, 'app');
const FEATURES_ROOT = path.join(WEB_ROOT, 'features');

const SOURCE_EXTENSIONS = new Set([
  '.ts',
  '.tsx',
  '.js',
  '.jsx',
  '.mjs',
  '.cjs',
]);

const violations = [];

function isWithin(parentDirectory, targetPath) {
  const relativePath = path.relative(parentDirectory, targetPath);

  return relativePath === ''
    || (
      relativePath !== '..'
      && !relativePath.startsWith(`..${path.sep}`)
      && !path.isAbsolute(relativePath)
    );
}

function isFile(filePath) {
  return existsSync(filePath) && statSync(filePath).isFile();
}

function collectSourceFiles(directory) {
  if (!existsSync(directory)) {
    return [];
  }

  const entries = readdirSync(directory, {
    withFileTypes: true,
  });

  return entries.flatMap((entry) => {
    const entryPath = path.join(directory, entry.name);

    if (entry.isDirectory()) {
      return collectSourceFiles(entryPath);
    }

    const extension = path.extname(entry.name);

    return SOURCE_EXTENSIONS.has(extension)
      ? [entryPath]
      : [];
  });
}

function resolveModulePath(importerPath, specifier) {
  let basePath;

  if (specifier.startsWith('@/')) {
    basePath = path.resolve(
      WEB_ROOT,
      specifier.slice(2),
    );
  } else if (specifier.startsWith('.')) {
    basePath = path.resolve(
      path.dirname(importerPath),
      specifier,
    );
  } else {
    return null;
  }

  const candidates = [
    basePath,
    `${basePath}.ts`,
    `${basePath}.tsx`,
    `${basePath}.js`,
    `${basePath}.jsx`,
    `${basePath}.mjs`,
    `${basePath}.cjs`,
    `${basePath}.css`,
    path.join(basePath, 'index.ts'),
    path.join(basePath, 'index.tsx'),
    path.join(basePath, 'index.js'),
    path.join(basePath, 'index.jsx'),
  ];

  return candidates.find(isFile) ?? null;
}

function extractImportSpecifiers(source) {
  const specifiers = new Set();

  const patterns = [
    /\b(?:import|export)\s+(?:type\s+)?(?:[\w*\s{},]+\s+from\s+)?['"]([^'"]+)['"]/g,
    /\bimport\s*\(\s*['"]([^'"]+)['"]\s*\)/g,
  ];

  for (const pattern of patterns) {
    for (const match of source.matchAll(pattern)) {
      specifiers.add(match[1]);
    }
  }

  return [...specifiers];
}

function featureNameFor(filePath) {
  if (!isWithin(FEATURES_ROOT, filePath)) {
    return null;
  }

  const relativePath = path.relative(
    FEATURES_ROOT,
    filePath,
  );

  return relativePath.split(path.sep)[0] || null;
}

function isPublicFeatureEntry(filePath, featureName) {
  return path.resolve(filePath) === path.join(
    FEATURES_ROOT,
    featureName,
    'index.ts',
  );
}

function addViolation(filePath, specifier, reason) {
  violations.push(
    `${path.relative(REPOSITORY_ROOT, filePath)}: `
      + `"${specifier}" — ${reason}`,
  );
}

function checkFeatureEntryPoints() {
  if (!existsSync(FEATURES_ROOT)) {
    addViolation(
      FEATURES_ROOT,
      '',
      'The features directory does not exist.',
    );

    return;
  }

  const featureDirectories = readdirSync(FEATURES_ROOT, {
    withFileTypes: true,
  }).filter((entry) => entry.isDirectory());

  for (const feature of featureDirectories) {
    const entryPoint = path.join(
      FEATURES_ROOT,
      feature.name,
      'index.ts',
    );

    if (!isFile(entryPoint)) {
      addViolation(
        entryPoint,
        '',
        `Feature "${feature.name}" must expose an index.ts public entry point.`,
      );
    }
  }
}

function checkImportBoundaries(filePath, source) {
  const importerFeature = featureNameFor(filePath);
  const importerIsApp = isWithin(APP_ROOT, filePath);

  const specifiers = extractImportSpecifiers(source);

  for (const specifier of specifiers) {
    if (
      specifier === '@/components/auth'
      || specifier.startsWith('@/components/auth/')
      || specifier === '@/lib/identity-api'
    ) {
      addViolation(
        filePath,
        specifier,
        'This import uses a retired Identity location.',
      );

      continue;
    }

    const resolvedPath = resolveModulePath(
      filePath,
      specifier,
    );

    if (!resolvedPath) {
      continue;
    }

    if (importerIsApp && isWithin(FEATURES_ROOT, resolvedPath)) {
      const targetFeature = featureNameFor(resolvedPath);

      const validPublicImport =
        specifier === `@/features/${targetFeature}`
        && isPublicFeatureEntry(resolvedPath, targetFeature);

      if (!validPublicImport) {
        addViolation(
          filePath,
          specifier,
          'Route files must import features through their public entry point.',
        );
      }

      continue;
    }

    if (!importerFeature) {
      continue;
    }

    if (isWithin(APP_ROOT, resolvedPath)) {
      addViolation(
        filePath,
        specifier,
        `Feature "${importerFeature}" must not depend on Next.js route implementation.`,
      );

      continue;
    }

    if (!isWithin(FEATURES_ROOT, resolvedPath)) {
      continue;
    }

    const targetFeature = featureNameFor(resolvedPath);

    if (targetFeature === importerFeature) {
      continue;
    }

    const validCrossFeatureImport =
      specifier === `@/features/${targetFeature}`
      && isPublicFeatureEntry(resolvedPath, targetFeature);

    if (!validCrossFeatureImport) {
      addViolation(
        filePath,
        specifier,
        `Feature "${importerFeature}" must use the public entry point of feature "${targetFeature}".`,
      );
    }
  }
}

function main() {
  checkFeatureEntryPoints();

  const sourceFiles = [
    ...collectSourceFiles(APP_ROOT),
    ...collectSourceFiles(FEATURES_ROOT),
  ];

  for (const filePath of sourceFiles) {
    const source = readFileSync(filePath, 'utf8');
    checkImportBoundaries(filePath, source);
  }

  if (violations.length > 0) {
    console.error('\nFrontend architecture violations:\n');

    for (const violation of violations) {
      console.error(`  - ${violation}`);
    }

    console.error(
      `\nFound ${violations.length} architecture violation(s).`,
    );

    process.exitCode = 1;
    return;
  }

  console.log(
    `[architecture] Checked ${sourceFiles.length} frontend source files.`,
  );

  console.log(
    '[architecture] Feature entry points and import boundaries are valid.',
  );
}

main();