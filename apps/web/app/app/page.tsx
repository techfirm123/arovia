
'use client';

import { useMemo, useState } from 'react';
import {
  Activity,
  ArrowDownRight,
  ArrowUpRight,
  Bell,
  Boxes,
  BriefcaseBusiness,
  CalendarDays,
  ChevronDown,
  ChevronRight,
  CircleAlert,
  ClipboardList,
  Clock3,
  Command,
  CreditCard,
  LayoutDashboard,
  Menu,
  MessageCircle,
  Package,
  Plus,
  Search,
  Settings2,
  ShoppingBag,
  Truck,
  Users,
  Wallet,
  Wrench,
  X,
  type LucideIcon,
} from 'lucide-react';

type NavigationItem = {
  label: string;
  icon: LucideIcon;
  description: string;
};

type NavigationGroup = {
  label: string;
  items: NavigationItem[];
};

const navigation: NavigationGroup[] = [
  {
    label: 'WORKSPACE',
    items: [
      { label: 'Overview', icon: LayoutDashboard, description: 'Your daily business pulse, priorities and activity.' },
      { label: 'Customers', icon: Users, description: 'Customer profiles, devices, history and follow-ups.' },
      { label: 'Sales & billing', icon: ShoppingBag, description: 'Enquiries, quotations, orders, invoices and payments.' },
      { label: 'Service desk', icon: Wrench, description: 'Repair intake, diagnosis, approvals and delivery.' },
    ],
  },
  {
    label: 'OPERATIONS',
    items: [
      { label: 'Products & inventory', icon: Boxes, description: 'Product catalogue, serial numbers and stock movements.' },
      { label: 'Purchasing', icon: Truck, description: 'Suppliers, purchase orders and goods receipts.' },
      { label: 'Finance', icon: Wallet, description: 'Collections, expenses, receivables and payables.' },
      { label: 'Communications', icon: MessageCircle, description: 'Customer conversations, WhatsApp and reminders.' },
    ],
  },
  {
    label: 'INTELLIGENCE',
    items: [
      { label: 'AI workforce', icon: BriefcaseBusiness, description: 'Specialized AI workers, recommendations and approvals.' },
      { label: 'Reports', icon: Activity, description: 'Sales, service, inventory and business performance.' },
    ],
  },
];

const settingsNavigationItem: NavigationItem = {
  label: 'Settings',
  icon: Settings2,
  description: 'Business profile, team access, preferences and integrations.',
};

const metrics = [
  { label: "Today's sales", value: '₹42,850', change: '+12.8%', direction: 'up' as const, icon: CreditCard, note: 'vs. previous business day' },
  { label: 'Open service jobs', value: '12', change: '3 due today', direction: 'down' as const, icon: Wrench, note: 'Across all repair stages' },
  { label: 'Pending quotations', value: '08', change: '₹1.24L', direction: 'up' as const, icon: ClipboardList, note: 'Total quoted value' },
  { label: 'Low stock items', value: '05', change: 'Needs review', direction: 'down' as const, icon: Package, note: 'At or below reorder level' },
];

const serviceJobs = [
  { id: 'SRV-1048', customer: 'Arjun Rao', device: 'Dell Inspiron 15', issue: 'Display flickering', status: 'Diagnosis', due: 'Today, 4:00 PM', tone: 'blue' },
  { id: 'SRV-1047', customer: 'Meera Nair', device: 'HP Pavilion', issue: 'Battery replacement', status: 'Waiting for parts', due: 'Tomorrow', tone: 'amber' },
  { id: 'SRV-1045', customer: 'Kiran Kumar', device: 'Custom Desktop', issue: 'No display / boot', status: 'Quality check', due: 'Today, 6:00 PM', tone: 'green' },
  { id: 'SRV-1042', customer: 'Ananya Shetty', device: 'Lenovo ThinkPad E14', issue: 'Keyboard replacement', status: 'In progress', due: '13 Nov', tone: 'purple' },
];

const activityItems = [
  { icon: ShoppingBag, tone: 'violet', title: 'New sale recorded', detail: 'Lenovo IdeaPad Slim 5 · INV-2084', time: '10 min ago' },
  { icon: Wrench, tone: 'blue', title: 'Repair moved to quality check', detail: 'SRV-1045 · Custom Desktop', time: '32 min ago' },
  { icon: Users, tone: 'green', title: 'New customer added', detail: 'Rohit Menon · Customer profile', time: '1 hour ago' },
  { icon: Truck, tone: 'amber', title: 'Purchase received', detail: 'PR-0312 · 8 stock units', time: '2 hours ago' },
];

const quickActions = [
  { label: 'New customer', target: 'Customers', icon: Users },
  { label: 'New sale', target: 'Sales & billing', icon: ShoppingBag },
  { label: 'New service job', target: 'Service desk', icon: Wrench },
  { label: 'Add product', target: 'Products & inventory', icon: Package },
];

function BrandMark() {
  return (
    <div className="brand-mark" aria-hidden="true">
      <span />
      <span />
      <span />
    </div>
  );
}

function SectionPlaceholder({ item }: { item: NavigationItem }) {
  const Icon = item.icon;

  return (
    <section className="placeholder-card">
      <div className="placeholder-icon"><Icon size={23} /></div>
      <span className="eyebrow">WORKSPACE MODULE</span>
      <h2>{item.label}</h2>
      <p>{item.description}</p>
      <div className="module-status"><span className="status-dot" /> Foundation module · Planned for an upcoming slice</div>
    </section>
  );
}

export default function HomePage() {
  const [activePage, setActivePage] = useState('Overview');
  const [quickCreateOpen, setQuickCreateOpen] = useState(false);
  const [notificationsOpen, setNotificationsOpen] = useState(false);
  const [searchOpen, setSearchOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [mobileNavOpen, setMobileNavOpen] = useState(false);

  const activeItem = useMemo(
    () => [...navigation.flatMap((group) => group.items), settingsNavigationItem].find((item) => item.label === activePage) ?? navigation[0].items[0],
    [activePage],
  );

  const filteredNavigation = useMemo(() => {
    const query = searchQuery.trim().toLowerCase();
    if (!query) return navigation;
    return navigation
      .map((group) => ({ ...group, items: group.items.filter((item) => item.label.toLowerCase().includes(query)) }))
      .filter((group) => group.items.length > 0);
  }, [searchQuery]);

  function navigateTo(label: string) {
    setActivePage(label);
    setMobileNavOpen(false);
    setQuickCreateOpen(false);
  }

  return (
    <main className="app-shell">
      <aside className={`sidebar ${mobileNavOpen ? 'sidebar-open' : ''}`}>
        <div className="brand-row">
          <BrandMark />
          <div className="brand-copy">
            <span className="brand-name">arovia</span>
            <span className="brand-caption">BUSINESS OS</span>
          </div>
          <button className="icon-button sidebar-close" aria-label="Close navigation" onClick={() => setMobileNavOpen(false)}><X size={18} /></button>
        </div>

        <button className="workspace-switcher" onClick={() => navigateTo('Overview')}>
          <div className="workspace-avatar">A</div>
          <div className="workspace-copy"><strong>Arovia Demo Store</strong><span>Owner workspace</span></div>
          <ChevronDown size={15} className="muted-icon" />
        </button>

        <nav className="side-navigation" aria-label="Main navigation">
          {filteredNavigation.map((group) => (
            <div className="nav-group" key={group.label}>
              <div className="nav-group-label">{group.label}</div>
              {group.items.map((item) => {
                const Icon = item.icon;
                const selected = activePage === item.label;
                return (
                  <button key={item.label} className={`nav-item ${selected ? 'nav-item-active' : ''}`} onClick={() => navigateTo(item.label)} aria-current={selected ? 'page' : undefined}>
                    <Icon size={17} strokeWidth={selected ? 2.1 : 1.8} />
                    <span>{item.label}</span>
                    {item.label === 'Service desk' && <span className="nav-count">12</span>}
                  </button>
                );
              })}
            </div>
          ))}
        </nav>

        <div className="sidebar-bottom">
          <button className={`nav-item ${activePage === 'Settings' ? 'nav-item-active' : ''}`} onClick={() => navigateTo('Settings')}>
            <Settings2 size={17} /><span>Settings</span>
          </button>
          <div className="sidebar-help">
            <div className="help-icon"><Command size={16} /></div>
            <div><strong>Need a hand?</strong><span>Explore the Arovia guide</span></div>
            <ChevronRight size={15} className="muted-icon" />
          </div>
          <div className="user-profile">
            <div className="user-avatar">DO</div>
            <div className="user-copy"><strong>Demo Owner</strong><span>Business owner</span></div>
            <ChevronDown size={16} className="muted-icon" />
          </div>
        </div>
      </aside>

      {mobileNavOpen && <button className="mobile-scrim" aria-label="Close navigation overlay" onClick={() => setMobileNavOpen(false)} />}

      <section className="main-panel">
        <header className="topbar">
          <div className="topbar-left">
            <button className="icon-button mobile-menu" aria-label="Open navigation" onClick={() => setMobileNavOpen(true)}><Menu size={20} /></button>
            <div className="breadcrumb"><span>Workspace</span><ChevronRight size={14} /><strong>{activePage}</strong></div>
          </div>
          <div className="topbar-actions">
            {searchOpen && <input autoFocus className="top-search-input" placeholder="Search Arovia..." value={searchQuery} onChange={(event) => setSearchQuery(event.target.value)} aria-label="Search navigation" />}
            <button className={`icon-button ${searchOpen ? 'icon-button-selected' : ''}`} aria-label="Search" onClick={() => { setSearchOpen((value) => !value); setSearchQuery(''); }}><Search size={18} /></button>
            <div className="popover-anchor">
              <button className={`icon-button notification-button ${notificationsOpen ? 'icon-button-selected' : ''}`} aria-label="Notifications" onClick={() => setNotificationsOpen((value) => !value)}><Bell size={18} /><span className="notification-dot" /></button>
              {notificationsOpen && <div className="small-popover notification-popover"><div className="popover-heading"><strong>Notifications</strong><span>3 new</span></div><p><CircleAlert size={15} /> 2 service jobs are due today.</p><p><Package size={15} /> 5 products need a stock review.</p><p><MessageCircle size={15} /> 1 customer follow-up is pending.</p></div>}
            </div>
            <div className="popover-anchor">
              <button className="primary-button" onClick={() => setQuickCreateOpen((value) => !value)}><Plus size={16} /><span>New</span><ChevronDown size={14} /></button>
              {quickCreateOpen && <div className="small-popover quick-create-popover"><div className="popover-heading"><strong>Quick create</strong></div>{quickActions.map((action) => { const Icon = action.icon; return <button key={action.label} className="quick-action" onClick={() => navigateTo(action.target)}><Icon size={16} /><span>{action.label}</span></button>; })}</div>}
            </div>
          </div>
        </header>

        <div className="page-content">
          {activePage === 'Overview' ? (
            <>
              <div className="page-heading-row">
                <div><div className="eyebrow">TODAY&apos;S SNAPSHOT</div><h1>Your business at a glance<span className="heading-period">.</span></h1><p className="page-subtitle">Here&apos;s what&apos;s happening across your business today.</p></div>
                <div className="date-button"><CalendarDays size={16} /><span>Today</span><ChevronDown size={14} /></div>
              </div>

              <div className="preview-notice"><span className="preview-spark">✦</span><span><strong>Product preview</strong> · Dashboard figures are illustrative sample data while we build the live business workspace.</span></div>

              <div className="metrics-grid">
                {metrics.map((metric) => {
                  const Icon = metric.icon;
                  return <article className="metric-card" key={metric.label}><div className="metric-top"><span>{metric.label}</span><span className="metric-icon"><Icon size={17} /></span></div><div className="metric-value">{metric.value}</div><div className="metric-bottom"><span className={`metric-change ${metric.direction === 'up' ? 'change-up' : 'change-neutral'}`}>{metric.direction === 'up' ? <ArrowUpRight size={14} /> : <ArrowDownRight size={14} />}{metric.change}</span><span className="metric-note">{metric.note}</span></div></article>;
                })}
              </div>

              <div className="dashboard-grid">
                <section className="panel service-panel">
                  <div className="panel-header"><div><h2>Service desk</h2><p>Keep repairs moving and customers informed.</p></div><button className="text-button" onClick={() => navigateTo('Service desk')}>View all <ArrowUpRight size={14} /></button></div>
                  <div className="service-table-wrap"><table className="service-table"><thead><tr><th>JOB / CUSTOMER</th><th>DEVICE & ISSUE</th><th>STATUS</th><th>DUE</th></tr></thead><tbody>{serviceJobs.map((job) => <tr key={job.id}><td><strong className="job-id">{job.id}</strong><span className="table-secondary">{job.customer}</span></td><td><strong>{job.device}</strong><span className="table-secondary">{job.issue}</span></td><td><span className={`status-pill status-${job.tone}`}>{job.status}</span></td><td className="due-cell">{job.due}</td></tr>)}</tbody></table></div>
                  <button className="panel-footer-link" onClick={() => navigateTo('Service desk')}>Open service desk <ChevronRight size={15} /></button>
                </section>

                <section className="panel activity-panel">
                  <div className="panel-header"><div><h2>Recent activity</h2><p>The latest across your workspace.</p></div><span className="activity-time-icon" aria-hidden="true"><Clock3 size={17} /></span></div>
                  <div className="activity-list">{activityItems.map((item) => { const Icon = item.icon; return <div className="activity-item" key={item.title}><div className={`activity-icon activity-${item.tone}`}><Icon size={16} /></div><div className="activity-copy"><strong>{item.title}</strong><span>{item.detail}</span></div><time>{item.time}</time></div>; })}</div>
                  <button className="panel-footer-link" onClick={() => navigateTo('Reports')}>View activity <ChevronRight size={15} /></button>
                </section>
              </div>

              <div className="bottom-grid">
                <section className="priority-card"><div className="priority-icon"><CircleAlert size={18} /></div><div className="priority-copy"><strong>3 things need your attention</strong><span>Review today&apos;s due repairs, low-stock items and a pending customer follow-up.</span></div><button onClick={() => navigateTo('Service desk')}>Review now <ArrowUpRight size={14} /></button></section>
                <section className="ai-card"><div className="ai-orb">✦</div><div className="ai-copy"><span className="eyebrow">AROVIA INTELLIGENCE</span><strong>Your AI workforce is getting ready.</strong><span>Business-aware assistance will appear here as we enable each worker.</span></div><span className="coming-soon">COMING SOON</span></section>
              </div>
            </>
          ) : (
            <div className="module-page"><div className="page-heading-row"><div><div className="eyebrow">AROVIA WORKSPACE</div><h1>{activePage}<span className="heading-period">.</span></h1><p className="page-subtitle">A connected part of your computer business operations.</p></div></div><SectionPlaceholder item={activeItem} /></div>
          )}

          <footer className="page-footer"><span>AROVIA · COMPUTER BUSINESS OS</span><span>Built around the way your business works.</span></footer>
        </div>
      </section>
    </main>
  );
}
