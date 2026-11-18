
import type { Metadata } from 'next';
import Link from 'next/link';
import {
  ArrowRight,
  ArrowUpRight,
  Boxes,
  BriefcaseBusiness,
  ChartNoAxesCombined,
  ClipboardCheck,
  Headphones,
  Laptop,
  Package,
  ShieldCheck,
  Sparkles,
  Users,
  Wrench,
} from 'lucide-react';

import styles from './page.module.css';

export const metadata: Metadata = {
  title: 'Arovia — Your business, working as one',
  description:
    'Arovia is an intelligent business operating platform being built for computer sales and service businesses.',
  applicationName: 'Arovia',
};

const capabilities = [
  {
    icon: Users,
    title: 'Customer relationships',
    description:
      'Keep customer details, device history and follow-ups connected in one place.',
  },
  {
    icon: Laptop,
    title: 'Sales and billing',
    description:
      'Bring enquiries, quotations, orders, invoices and payments into one workflow.',
  },
  {
    icon: Wrench,
    title: 'Service operations',
    description:
      'Track repair intake, diagnosis, approvals, parts and delivery from start to finish.',
  },
  {
    icon: Boxes,
    title: 'Inventory and purchasing',
    description:
      'Understand products, serial numbers, stock movements and supplier purchases.',
  },
  {
    icon: ChartNoAxesCombined,
    title: 'Business visibility',
    description:
      'Build a clearer picture of sales, service activity, inventory and performance.',
  },
  {
    icon: BriefcaseBusiness,
    title: 'AI workforce',
    description:
      'Move towards business-aware AI assistance that can help with everyday work.',
  },
];

const workflow = [
  {
    number: '01',
    title: 'Know your customers',
    description:
      'Connect customer profiles with the devices they own, their purchases and service history.',
  },
  {
    number: '02',
    title: 'Keep work moving',
    description:
      'Connect sales, repairs, inventory and payments instead of managing disconnected records.',
  },
  {
    number: '03',
    title: 'Make better decisions',
    description:
      'Bring operational information together to understand what needs attention.',
  },
];

function BrandMark() {
  return (
    <span className={styles.brandMark} aria-hidden="true">
      <span />
      <span />
      <span />
    </span>
  );
}

export default function HomePage() {
  return (
    <main className={styles.page}>
      <header className={styles.header}>
        <div className={styles.headerInner}>
          <Link href="/" className={styles.brand} aria-label="Arovia home">
            <BrandMark />
            <span className={styles.brandName}>arovia</span>
          </Link>

          <nav className={styles.navigation} aria-label="Main navigation">
            <a href="#platform">Platform</a>
            <a href="#how-it-works">How it works</a>
            <a href="#intelligence">Intelligence</a>
          </nav>

          <div className={styles.headerActions}>
            <Link href="/login" className={styles.signInLink}>
              Sign in
            </Link>
            <Link href="/signup" className={styles.headerCta}>
              Get started <ArrowUpRight size={15} />
            </Link>
          </div>
        </div>
      </header>

      <section className={styles.hero}>
        <div className={styles.heroInner}>
          <div className={styles.heroCopy}>
            <div className={styles.announcement}>
              <Sparkles size={14} />
              <span>BUILT FOR THE WAY YOUR BUSINESS WORKS</span>
            </div>

            <h1>
              Your business deserves
              <br />
              its own <span>intelligent</span>
              <br />
              workforce.
            </h1>

            <p className={styles.heroDescription}>
              Arovia brings your customers, sales, service operations,
              inventory and business intelligence together — so you
              can spend less time coordinating work and more time
              growing your business.
            </p>

            <div className={styles.heroActions}>
              <Link href="/signup" className={styles.primaryCta}>
                Get started with Arovia
                <ArrowRight size={17} />
              </Link>

              <Link href="/login" className={styles.secondaryCta}>
                I already have an account
              </Link>
            </div>

            <div className={styles.heroFootnote}>
              <ShieldCheck size={15} />
              <span>Secure account access · Built around your business</span>
            </div>
          </div>

          <div className={styles.heroVisual} aria-label="Illustration of connected business operations">
            <div className={styles.visualGlow} />

            <div className={styles.visualTopline}>
              <span className={styles.visualDot} />
              <span>THE BUSINESS WORKSPACE</span>
              <span className={styles.liveLabel}>CONNECTED</span>
            </div>

            <div className={styles.visualBrand}>
              <BrandMark />
              <div>
                <strong>One connected business</strong>
                <span>Your work, in context</span>
              </div>
            </div>

            <div className={styles.visualConnections}>
              <div className={`${styles.connectionCard} ${styles.customerCard}`}>
                <span className={styles.connectionIcon}><Users size={18} /></span>
                <span><strong>Customers</strong><small>Relationships & history</small></span>
              </div>

              <div className={`${styles.connectionCard} ${styles.salesCard}`}>
                <span className={styles.connectionIcon}><ClipboardCheck size={18} /></span>
                <span><strong>Sales</strong><small>Orders & billing</small></span>
              </div>

              <div className={styles.coreNode}>
                <BrandMark />
                <strong>Arovia</strong>
                <small>Business OS</small>
              </div>

              <div className={`${styles.connectionCard} ${styles.serviceCard}`}>
                <span className={styles.connectionIcon}><Wrench size={18} /></span>
                <span><strong>Service desk</strong><small>Repairs & delivery</small></span>
              </div>

              <div className={`${styles.connectionCard} ${styles.inventoryCard}`}>
                <span className={styles.connectionIcon}><Package size={18} /></span>
                <span><strong>Inventory</strong><small>Products & stock</small></span>
              </div>
            </div>

            <div className={styles.visualBottom}>
              <span><Sparkles size={14} /> Intelligence across your operations</span>
              <span className={styles.visualArrow}><ArrowUpRight size={15} /></span>
            </div>
          </div>
        </div>

        <div className={styles.heroBottomLine}>
          <span>ONE BUSINESS</span>
          <span className={styles.heroLine} />
          <span>CONNECTED OPERATIONS</span>
          <span className={styles.heroLine} />
          <span>INTELLIGENT WORKFLOWS</span>
        </div>
      </section>

      <section className={styles.audience}>
        <div className={styles.sectionContainer}>
          <p className={styles.sectionEyebrow}>STARTING WITH A REAL BUSINESS</p>
          <div className={styles.audienceContent}>
            <div>
              <h2>Designed around the realities of computer sales and service.</h2>
            </div>
            <p>
              From selling a laptop to managing a repair, tracking a
              replacement part or following up with a customer, Arovia
              is being shaped around the everyday work of a computer
              business.
            </p>
          </div>
          <div className={styles.audienceTags}>
            <span><Laptop size={15} /> Computer retail</span>
            <span><Wrench size={15} /> Repair and service</span>
            <span><Package size={15} /> Parts and inventory</span>
            <span><Headphones size={15} /> Customer support</span>
          </div>
        </div>
      </section>

      <section className={styles.capabilities} id="platform">
        <div className={styles.sectionContainer}>
          <div className={styles.sectionHeading}>
            <p className={styles.sectionEyebrow}>THE PLATFORM</p>
            <h2>Everything connected.<br />Nothing working in isolation.</h2>
            <p>
              Arovia is being developed as a unified operating layer
              for the workflows that keep a computer business running.
            </p>
          </div>

          <div className={styles.capabilityGrid}>
            {capabilities.map((capability, index) => {
              const Icon = capability.icon;

              return (
                <article className={styles.capabilityCard} key={capability.title}>
                  <div className={styles.capabilityIcon}>
                    <Icon size={20} />
                  </div>
                  <span className={styles.cardNumber}>
                    {String(index + 1).padStart(2, '0')}
                  </span>
                  <h3>{capability.title}</h3>
                  <p>{capability.description}</p>
                </article>
              );
            })}
          </div>
        </div>
      </section>

      <section className={styles.workflow} id="how-it-works">
        <div className={styles.sectionContainer}>
          <div className={styles.workflowHeading}>
            <div>
              <p className={styles.sectionEyebrow}>A MORE CONNECTED WAY TO WORK</p>
              <h2>From daily tasks<br />to the bigger picture.</h2>
            </div>
            <p>
              Business operations become easier to understand when
              the information and workflows behind them are connected.
            </p>
          </div>

          <div className={styles.workflowGrid}>
            {workflow.map((item) => (
              <article className={styles.workflowCard} key={item.number}>
                <span>{item.number}</span>
                <h3>{item.title}</h3>
                <p>{item.description}</p>
              </article>
            ))}
          </div>
        </div>
      </section>

      <section className={styles.intelligence} id="intelligence">
        <div className={styles.intelligenceInner}>
          <div className={styles.intelligenceOrb}>
            <Sparkles size={30} />
          </div>
          <div className={styles.intelligenceCopy}>
            <p className={styles.sectionEyebrow}>AROVIA INTELLIGENCE</p>
            <h2>Not just another tool.<br />A more capable way to run your business.</h2>
            <p>
              Arovia&apos;s long-term direction is an intelligent workforce
              that understands business context, helps surface what
              matters and supports people in getting work done.
              AI will complement your operations, not become a
              prerequisite for running them.
            </p>
          </div>
          <div className={styles.intelligenceNote}>
            <Sparkles size={16} />
            <span>Built progressively, with human control.</span>
          </div>
        </div>
      </section>

      <section className={styles.finalCta}>
        <div className={styles.sectionContainer}>
          <p className={styles.sectionEyebrow}>YOUR BUSINESS. YOUR WORKSPACE.</p>
          <h2>Bring your business<br />into one place.</h2>
          <p>
            Create your Arovia account and take the first step
            towards more connected business operations.
          </p>
          <Link href="/signup" className={styles.primaryCta}>
            Get started with Arovia
            <ArrowRight size={17} />
          </Link>
        </div>
      </section>

      <footer className={styles.footer}>
        <div className={styles.footerInner}>
          <Link href="/" className={styles.brand}>
            <BrandMark />
            <span className={styles.brandName}>arovia</span>
          </Link>
          <span>Business operations, connected.</span>
          <div>
            <Link href="/login">Sign in</Link>
            <Link href="/signup">Create account</Link>
          </div>
          <small>© {new Date().getFullYear()} Arovia</small>
        </div>
      </footer>
    </main>
  );
}
