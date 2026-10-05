import { useEffect, useState } from 'react'
import {
  Activity, ArrowRight, ArrowUpRight, Bell, BookOpen, BrainCircuit, Check,
  ChevronRight, CircleHelp, Clock3, Code2,
  FileText, Flame, Gauge, GraduationCap, LayoutDashboard, LockKeyhole, LogOut,
  Menu, MoreHorizontal, Search, Send, Settings2, ShieldCheck, Target,
  TrendingUp, UserRound, Users, X, Zap,
} from 'lucide-react'
import { Link, Navigate, Route, Routes, useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { api, apiErrorMessage } from './api'

const navItems = [
  { path: '/dashboard', label: 'Overview', icon: LayoutDashboard },
  { path: '/my-skills', label: 'My skills', icon: Zap },
  { path: '/assessment', label: 'Assessment', icon: Target },
  { path: '/gap-analysis', label: 'Skill gap', icon: Activity },
  { path: '/learning', label: 'Learning', icon: BookOpen },
  { path: '/assistant', label: 'AI assistant', icon: BrainCircuit },
]

function App() {
  const location = useLocation()
  const navigate = useNavigate()
  const [user, setUser] = useState(() => {
    try {
      return JSON.parse(localStorage.getItem('skillsync.user'))
    } catch {
      return null
    }
  })
  const [notice, setNotice] = useState('')
  const [mobileNavOpen, setMobileNavOpen] = useState(false)
  const [authChecking, setAuthChecking] = useState(Boolean(localStorage.getItem('skillsync.token')))
  const isAuthPage = ['/login', '/register'].includes(location.pathname)

  useEffect(() => {
    if (!notice) return undefined
    const timer = window.setTimeout(() => setNotice(''), 3500)
    return () => window.clearTimeout(timer)
  }, [notice])

  useEffect(() => {
    if (!localStorage.getItem('skillsync.token')) return
    let active = true
    api.get('/api/users/me')
      .then(({ data }) => {
        if (!active) return
        setUser(data)
        localStorage.setItem('skillsync.user', JSON.stringify(data))
      })
      .catch(() => {
        if (!active) return
        localStorage.removeItem('skillsync.token')
        localStorage.removeItem('skillsync.user')
        setUser(null)
      })
      .finally(() => active && setAuthChecking(false))
    return () => { active = false }
  }, [])

  async function login(form) {
    localStorage.removeItem('skillsync.token')
    try {
      const { data } = await api.post('/api/auth/login', form)
      localStorage.setItem('skillsync.token', data.token)
      const { data: profile } = await api.get('/api/users/me')
      localStorage.setItem('skillsync.user', JSON.stringify(profile))
      setUser(profile)
      navigate('/dashboard')
      setNotice(`Welcome back, ${profile.name.split(' ')[0]}!`)
    } catch (error) {
      localStorage.removeItem('skillsync.token')
      throw error
    }
  }

  async function register(form) {
    await api.post('/api/auth/register', { ...form, role: 'EMPLOYEE' })
    setNotice('Account created! Sign in with your new credentials.')
    navigate('/login')
  }

  function logout() {
    localStorage.removeItem('skillsync.token')
    localStorage.removeItem('skillsync.user')
    setUser(null)
    navigate('/login')
    setNotice('You’ve been signed out.')
  }

  function goTo(path) {
    setMobileNavOpen(false)
    navigate(path)
  }

  if (isAuthPage) {
    if (authChecking) return <AuthLoading />
    if (user) return <Navigate to="/dashboard" replace />
    return (
      <>
        <Routes>
          <Route path="/login" element={<AuthPage mode="login" onSubmit={login} notice={notice} />} />
          <Route path="/register" element={<AuthPage mode="register" onSubmit={register} notice={notice} />} />
        </Routes>
      </>
    )
  }

  if (authChecking) return <AuthLoading />
  if (!user || !localStorage.getItem('skillsync.token')) return <Navigate to="/login" replace />

  const visibleNavItems = navItems.filter(({ path }) => {
    if (user.role === 'ADMIN') return true
    if (user.role === 'MANAGER') return path !== '/assessment'
    if (user.role === 'SME') return path !== '/gap-analysis'
    return true
  })

  return (
    <div className="app-shell">
      <aside className={`sidebar ${mobileNavOpen ? 'sidebar-open' : ''}`}>
        <div className="brand">
          <span className="brand-mark"><Activity size={19} strokeWidth={2.5} /></span>
          <span>Skill<span className="brand-light">Sync</span></span>
        </div>
        <div className="workspace-label">WORKSPACE <button aria-label="Workspace settings"><MoreHorizontal size={17} /></button></div>
        <nav className="side-nav">
          {visibleNavItems.map(({ path, label, icon: Icon }) => (
            <Link key={path} to={path} onClick={() => setMobileNavOpen(false)} className={`nav-link ${location.pathname === path ? 'active' : ''}`}>
              <Icon size={18} strokeWidth={1.8} /><span>{label}</span>
              {path === '/assessment' && <span className="nav-new">2</span>}
            </Link>
          ))}
        </nav>
        <div className="sidebar-bottom">
          <div className="sidebar-promo">
            <div className="promo-icon"><BrainCircuit size={17} /></div>
            <strong>Your next level awaits</strong>
            <p>You're on a 6-day learning streak. Keep it going!</p>
            <div className="streak-row"><span>WEEKLY GOAL</span><span>4 / 5 days</span></div>
            <div className="mini-progress"><i style={{ width: '80%' }} /></div>
          </div>
          <button className="nav-link help-link" onClick={() => setNotice('Your learning journey is looking good!')}>
            <CircleHelp size={18} /><span>Help & support</span>
          </button>
          <button className="profile-mini" onClick={() => goTo('/profile')}>
            <Avatar name={user.name} />
            <span className="profile-mini-copy"><strong>{user.name}</strong><small>Product designer</small></span>
            <MoreHorizontal size={18} className="profile-more" />
          </button>
        </div>
      </aside>
      {mobileNavOpen && <button className="mobile-scrim" aria-label="Close navigation" onClick={() => setMobileNavOpen(false)} />}
      <main className="main-area">
        <header className="topbar">
          <button className="mobile-menu" aria-label="Open navigation" onClick={() => setMobileNavOpen(true)}><Menu size={21} /></button>
          <div className="breadcrumb"><span>Workspace</span><ChevronRight size={15} /><strong>{navItems.find(item => item.path === location.pathname)?.label || (location.pathname === '/profile' ? 'Profile' : 'Overview')}</strong></div>
          <div className="topbar-right">
            <div className="search-wrap"><Search size={16} /><input aria-label="Search" placeholder="Search anything..." /><kbd>⌘ K</kbd></div>
            <button className="icon-button notification-button" aria-label="Notifications" onClick={() => setNotice('You’re all caught up!')}><Bell size={18} /><i /></button>
            <button className="topbar-avatar" onClick={() => navigate('/profile')} aria-label="Open profile"><Avatar name={user.name} /></button>
          </div>
        </header>
        <div className="page-content">
          <Routes>
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
            <Route path="/dashboard" element={<Dashboard user={user} navigate={goTo} />} />
            <Route path="/profile" element={<ProfilePage user={user} setUser={setUser} setNotice={setNotice} />} />
            <Route path="/my-skills" element={<SkillsPage setNotice={setNotice} />} />
            <Route path="/assessment" element={<AssessmentPage setNotice={setNotice} />} />
            <Route path="/gap-analysis" element={<GapPage />} />
            <Route path="/learning" element={<LearningPage setNotice={setNotice} />} />
            <Route path="/assistant" element={<AssistantPage />} />
            <Route path="*" element={<Navigate to="/dashboard" replace />} />
          </Routes>
        </div>
      </main>
      {notice && <div className="toast"><span className="toast-check"><Check size={14} /></span>{notice}<button onClick={() => setNotice('')} aria-label="Dismiss"><X size={15} /></button></div>}
      <button className="logout-fab" onClick={logout} aria-label="Sign out" title="Sign out"><LogOut size={17} /></button>
    </div>
  )
}

function Avatar({ name = 'Alex Morgan', size = 'normal' }) {
  const initials = name.split(/\s+/).map(part => part[0]).slice(0, 2).join('').toUpperCase()
  return <span className={`avatar avatar-${size}`}>{initials}</span>
}

function AuthLoading() {
  return <div className="auth-loading"><span className="spinner" /> Checking your SkillSync session…</div>
}

function LoadState({ error, loading, retry, empty, children }) {
  if (loading) return <div className="panel state-panel"><span className="spinner" /><span>Loading your SkillSync data…</span></div>
  if (error) return <div className="panel state-panel state-error"><strong>We couldn’t load this data.</strong><span>{error}</span><button className="button button-secondary" onClick={retry}>Try again</button></div>
  if (empty) return <div className="empty-state"><span><BookOpen size={17} /></span><strong>Nothing here yet</strong><p>Your data will show up here as you get started.</p></div>
  return children
}

function PageHeading({ eyebrow, title, description, action }) {
  return (
    <div className="page-heading">
      <div><div className="eyebrow">{eyebrow}</div><h1>{title}</h1><p>{description}</p></div>
      {action}
    </div>
  )
}

function toUiSkill(skill, index = 0) {
  return {
    ...skill,
    id: skill.skillId ?? skill.id,
    mappingId: skill.employeeSkillId ?? skill.mappingId ?? (skill.skillId ? skill.id : undefined),
    name: skill.skillName ?? skill.name,
    level: Number(skill.proficiency ?? skill.currentProficiency ?? 0) * 10,
    category: skill.category || 'General',
    color: ['violet', 'blue', 'green', 'pink', 'amber', 'cyan'][index % 6],
    trend: skill.trend || '',
  }
}

function Dashboard({ user, navigate }) {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  async function load() {
    setLoading(true)
    setError('')
    try {
      const response = await api.get('/api/dashboard')
      setData(response.data)
    } catch (requestError) {
      setError(apiErrorMessage(requestError))
    } finally {
      setLoading(false)
    }
  }
  useEffect(() => { load() }, [])

  if (loading || error) {
    return <><PageHeading eyebrow="YOUR SKILLSYNC WORKSPACE" title={`Welcome, ${user.name.split(' ')[0]}`} description="Your workspace is loading your latest progress." /><LoadState loading={loading} error={error} retry={load} /></>
  }
  if (user.role !== 'EMPLOYEE') return <RoleDashboard user={user} data={data} navigate={navigate} />

  const skills = (data.skills || []).map(toUiSkill)
  const average = skills.length ? Math.round(skills.reduce((sum, skill) => sum + skill.level, 0) / skills.length) : 0
  const assessments = data.assessments || []
  const requests = data.trainingRequests || []
  const recommendation = (data.recommendations || [])[0]
  return (
    <>
      <section className="career-hero">
        <div className="career-hero-copy">
          <span className="career-hero-kicker"><Activity size={13} /> YOUR PERSONAL GROWTH OVERVIEW</span>
          <h1>Make your next<br />move <span>count.</span></h1>
          <p>Good morning, {user.name.split(' ')[0]}. See your skills clearly, close your gaps confidently, and keep your career moving forward.</p>
          <div className="career-hero-actions">
            <button className="button button-lime" onClick={() => navigate('/assessment')}><Target size={15} /> Take assessment <ArrowRight size={14} /></button>
            <button className="career-hero-link" onClick={() => navigate('/assistant')}>Ask your AI career coach <ArrowUpRight size={14} /></button>
          </div>
        </div>
        <div className="career-signal" aria-label={skills.length ? `Average recorded skill proficiency ${average} percent` : 'No skills recorded yet'}>
          <span className="career-signal-label">YOUR SKILL PROFILE</span>
          <div className="career-signal-ring" style={{ '--profile-progress': `${average}%` }}>
            <div><strong>{skills.length ? `${average}%` : '—'}</strong><span>{skills.length ? 'CURRENT LEVEL' : 'ADD A SKILL'}</span></div>
          </div>
          <span className="career-signal-foot">{skills.length} {skills.length === 1 ? 'skill' : 'skills'} in your profile</span>
          <span className="career-orbit orbit-a" />
          <span className="career-orbit orbit-b" />
        </div>
        <span className="career-hero-glow" aria-hidden="true" />
      </section>
      <section className="stats-grid">
        <StatCard label="Overall skill score" value={`${average}%`} change={skills.length ? `${skills.length} tracked` : 'Start here'} icon={Gauge} color="violet" sub="current proficiency" />
        <StatCard label="Skills tracked" value={String(skills.length).padStart(2, '0')} change={skills.length ? 'Up to date' : 'Add a skill'} icon={TrendingUp} color="blue" sub="in your profile" />
        <StatCard label="Assessments" value={String(assessments.length).padStart(2, '0')} change={assessments.length ? 'Submitted' : 'Not started'} icon={Clock3} color="pink" sub="recorded submissions" />
        <StatCard label="Training requests" value={String(requests.length).padStart(2, '0')} change={requests.filter(item => item.status !== 'COMPLETED').length ? 'In progress' : 'On track'} icon={Flame} color="amber" sub="all statuses" />
      </section>
      <section className="dashboard-grid">
        <div className="panel growth-panel">
          <div className="panel-header"><div><h2>Your skills at a glance</h2><p>Current proficiency across your tracked skills</p></div><button className="select-button" onClick={() => navigate('/my-skills')}>View skills <ArrowRight size={14} /></button></div>
          <LoadState empty={!skills.length}>
            <div className="focus-list dashboard-skills">{skills.slice(0, 5).map((skill, index) => <div className="focus-item" key={skill.id}><span className={`focus-icon ${skill.color}`}><SkillSymbol index={index} /></span><div className="focus-copy"><div><strong>{skill.name}</strong><span>{skill.level}%</span></div><Progress value={skill.level} color={skill.color} /></div></div>)}</div>
          </LoadState>
        </div>
        <div className="panel focus-panel">
          <div className="panel-header"><div><h2>Training requests</h2><p>Progress with a subject matter expert</p></div><button className="more-btn" aria-label="Open learning" onClick={() => navigate('/learning')}><ArrowUpRight size={17} /></button></div>
          <LoadState empty={!requests.length}>
            <div className="request-list">{requests.slice(0, 4).map(item => <div className="request-row" key={item.id}><span className={`status-dot status-${String(item.status).toLowerCase()}`} /><span className="request-name">{item.skillName}</span><span className="status-pill">{item.status.replaceAll('_', ' ')}</span></div>)}</div>
            <Link className="panel-link" to="/learning">Explore learning <ArrowRight size={15} /></Link>
          </LoadState>
        </div>
      </section>
      <section className="dashboard-grid lower-grid">
        <div className="panel goals-panel">
          <div className="panel-header"><div><h2>Recent assessments</h2><p>Latest submitted skill scores</p></div><div className="goal-badge"><Target size={14} /> {assessments.length} total</div></div>
          <LoadState empty={!assessments.length}>
            <div className="assessment-history">{assessments.slice(0, 4).map(item => <div key={item.id}><span>{item.skillName}</span><strong>{item.score}/10</strong><small>{String(item.assessmentType).replaceAll('_', ' ')}</small></div>)}</div>
          </LoadState>
        </div>
        <div className="panel recommendations-panel">
          <div className="panel-header"><div><h2>Recommended for you</h2><p>Picked to help you grow</p></div><button className="more-btn" aria-label="More options"><MoreHorizontal size={19} /></button></div>
          {recommendation ? <div className="recommendation-card"><div className="course-art violet-art"><Code2 size={21} /><span className="art-orbit" /></div><div className="recommendation-info"><span className="tag">SKILL GAP · {recommendation.estimatedHours}H</span><strong>{recommendation.title}</strong><small>{recommendation.gap} proficiency points to your goal</small></div><button className="round-arrow" aria-label="Open learning" onClick={() => navigate('/learning')}><ArrowUpRight size={17} /></button></div> : <LoadState empty />}
          <div className="recommendation-bottom"><span><Target size={14} /> Matches your goals</span><button onClick={() => navigate('/learning')}>Explore all <ArrowRight size={14} /></button></div>
        </div>
      </section>
    </>
  )
}

function RoleDashboard({ user, data, navigate }) {
  const [error, setError] = useState('')
  const [selectedRequest, setSelectedRequest] = useState('')
  const [sessionTitle, setSessionTitle] = useState('')
  const [sessionScheduledAt, setSessionScheduledAt] = useState('')
  const [skillName, setSkillName] = useState('')
  const [skillCategory, setSkillCategory] = useState('')
  const [jobRoleName, setJobRoleName] = useState('')
  const [jobRoleDescription, setJobRoleDescription] = useState('')
  const [requiredSkillId, setRequiredSkillId] = useState('')
  const [requiredLevel, setRequiredLevel] = useState(5)
  const [pending, setPending] = useState(false)
  const [userRoles, setUserRoles] = useState({})

  async function runAction(action, _successMessage) {
    setError('')
    setPending(true)
    try {
      await action()
      setError('')
      window.location.reload()
    } catch (requestError) {
      setError(apiErrorMessage(requestError))
    } finally {
      setPending(false)
    }
  }

  if (user.role === 'ADMIN') {
    const users = data.users || []
    const skills = data.skills || []
    const roles = data.jobRoles || []
    const managers = users.filter(member => member.role === 'MANAGER')
    const managerAssignments = data.managerAssignments || []
    return <>
      <PageHeading eyebrow="SYSTEM OVERVIEW" title="Admin dashboard" description="Manage workspace users, skills, and career pathways." action={<button className="button button-secondary" onClick={() => downloadWorkforceReport(data.workforceReport || [])}><FileText size={15} /> Export readiness</button>} />
      {error && <div className="auth-error">{error}</div>}
      <section className="stats-grid">
        <StatCard label="Workspace users" value={users.length} change="Active accounts" icon={Users} color="violet" sub="all roles" />
        <StatCard label="Skills catalog" value={skills.length} change="Available" icon={Zap} color="blue" sub="skill definitions" />
        <StatCard label="Job pathways" value={roles.length} change="Configured" icon={Target} color="pink" sub="career tracks" />
        <StatCard label="Active training" value={data.openTrainingRequests || 0} change="Requests" icon={BookOpen} color="amber" sub="open or in progress" />
      </section>
      <div className="dashboard-grid">
        <div className="panel role-panel"><div className="panel-header"><div><h2>People & access</h2><p>Update workspace roles. Access changes apply immediately.</p></div></div>
          <div className="data-table"><div className="table-head team-table-head"><span>MEMBER</span><span>ROLE</span><span>TEAM MANAGER</span></div>
            {users.map(member => {
              const assignment = managerAssignments.find(item => Number(item.employeeId) === Number(member.id))
              return <div className="table-row team-table-row" key={member.id}><div className="member-cell"><Avatar name={member.name} /><span><strong>{member.name}</strong><small>{member.email}</small></span></div><span className="table-role">{member.role}</span><div className="team-access-controls"><select aria-label={`Change role for ${member.name}`} value={userRoles[member.id] || member.role} onChange={event => { const role = event.target.value; setUserRoles({ ...userRoles, [member.id]: role }); runAction(() => api.put(`/api/users/${member.id}/role`, { role }), 'Member role updated.') }} disabled={pending || Number(member.id) === Number(user.id)}><option value="EMPLOYEE">Employee</option><option value="SME">SME</option><option value="MANAGER">Manager</option><option value="ADMIN">Admin</option></select>{member.role === 'EMPLOYEE' && <select aria-label={`Assign manager for ${member.name}`} value={assignment?.managerId || ''} onChange={event => runAction(() => api.put(`/api/manager-employees/${member.id}`, { managerId: Number(event.target.value) }), 'Manager assignment updated.')} disabled={pending || !managers.length} required><option value="">Assign manager</option>{managers.map(manager => <option key={manager.id} value={manager.id}>{manager.name}</option>)}</select>}</div></div>
            })}
            {!users.length && <div className="empty-state"><span><Users size={16} /></span><strong>No workspace users yet</strong></div>}
          </div>
        </div>
        <div className="panel role-panel"><div className="panel-header"><div><h2>Skills catalog</h2><p>Add skills employees can assess and track.</p></div></div>
          <form className="inline-create" onSubmit={event => { event.preventDefault(); runAction(() => api.post('/api/skills', { name: skillName, category: skillCategory }), 'Skill added to the catalog.') }}>
            <input placeholder="Skill name" value={skillName} onChange={event => setSkillName(event.target.value)} required />
            <input placeholder="Category" value={skillCategory} onChange={event => setSkillCategory(event.target.value)} />
            <button className="button button-primary" disabled={pending}>Add skill</button>
          </form>
          <div className="catalog-list">{skills.slice(0, 8).map(skill => <div key={skill.id}><span><strong>{skill.name}</strong><small>{skill.category || 'General'}</small></span><button className="text-danger" disabled={pending} onClick={() => runAction(() => api.delete(`/api/skills/${skill.id}`), 'Skill removed.')}>Remove</button></div>)}</div>
        </div>
      </div>
      <div className="panel role-panel"><div className="panel-header"><div><h2>Career pathways</h2><p>Job roles and the skills they require.</p></div><button className="button button-secondary" onClick={() => navigate('/my-skills')}>Manage skills <ArrowRight size={14} /></button></div>
        <form className="inline-create" onSubmit={event => { event.preventDefault(); runAction(() => api.post('/api/job-roles', { name: jobRoleName, description: jobRoleDescription }), 'Career pathway created.') }}>
          <input placeholder="New job role" value={jobRoleName} onChange={event => setJobRoleName(event.target.value)} required />
          <input placeholder="Description (optional)" value={jobRoleDescription} onChange={event => setJobRoleDescription(event.target.value)} />
          <button className="button button-primary" disabled={pending}>Add pathway</button>
        </form>
        <div className="role-cards">{roles.map(role => <div className="role-card role-card-manage" key={role.id}><span className="summary-icon violet"><Target size={16} /></span><strong>{role.name}</strong><small>{role.requiredSkillCount} required skills</small><form className="required-skill-form" onSubmit={event => { event.preventDefault(); runAction(() => api.put(`/api/job-roles/${role.id}/required-skills`, { skillId: Number(requiredSkillId), requiredLevel: Number(requiredLevel), importance: 5 }), 'Required skill mapping saved.') }}><select aria-label={`Skill required for ${role.name}`} value={requiredSkillId} onChange={event => setRequiredSkillId(event.target.value)} required><option value="">Select skill</option>{skills.map(skill => <option key={skill.id} value={skill.id}>{skill.name}</option>)}</select><select aria-label={`Required level for ${role.name}`} value={requiredLevel} onChange={event => setRequiredLevel(event.target.value)}>{Array.from({ length: 10 }, (_, index) => <option key={index + 1} value={index + 1}>Level {index + 1}</option>)}</select><button className="button button-secondary small" disabled={pending || !skills.length}>Map skill</button></form></div>)}</div>
        {!roles.length && <EmptyInline text="No career pathways yet. Create one above." />}
      </div>
      <div className="panel role-panel"><div className="panel-header"><div><h2>Workforce readiness</h2><p>Current proficiency and development areas across employees.</p></div></div>
        <div className="data-table"><div className="table-head report-head"><span>EMPLOYEE</span><span>SKILLS</span><span>AVERAGE</span><span>FOCUS AREAS</span></div>
          {(data.workforceReport || []).map(member => <div className="table-row report-row" key={member.employeeId}><div className="member-cell"><Avatar name={member.name} /><span><strong>{member.name}</strong><small>{member.email}</small></span></div><span>{member.skillsTracked}</span><span>{Number(member.averageProficiency) * 10}%</span><span>{member.developmentAreas}</span></div>)}
          {!(data.workforceReport || []).length && <EmptyInline text="No employee readiness data available yet." />}
        </div>
      </div>
    </>
  }

  if (user.role === 'MANAGER') {
    const team = data.team || []
    return <>
      <PageHeading eyebrow="TEAM DEVELOPMENT" title="Manager dashboard" description="Understand team readiness and focus development where it matters." action={<button className="button button-secondary" onClick={() => downloadWorkforceReport(data.workforceReport || [])}><FileText size={15} /> Export readiness</button>} />
      <section className="stats-grid">
        <StatCard label="Team members" value={team.length} change="Direct reports" icon={Users} color="violet" sub="assigned to you" />
        <StatCard label="Average readiness" value={`${team.length ? Math.round(team.reduce((sum, item) => sum + Number(item.readiness || 0), 0) / team.length) : 0}%`} change="Current score" icon={Gauge} color="blue" sub="team proficiency" />
        <StatCard label="Development areas" value={(data.workforceReport || []).reduce((sum, item) => sum + Number(item.developmentAreas || 0), 0)} change="Across your team" icon={Activity} color="pink" sub="skills under 5/10" />
        <StatCard label="Open training" value={(data.openTrainingRequests || []).length} change="Needs attention" icon={BookOpen} color="amber" sub="team requests" />
      </section>
      <div className="panel role-panel"><div className="panel-header"><div><h2>Team readiness</h2><p>Current skill coverage and proficiency by employee.</p></div></div>
        <div className="data-table"><div className="table-head manager-head"><span>EMPLOYEE</span><span>SKILLS TRACKED</span><span>READINESS</span><span>PROFILE</span></div>
          {team.map(member => <div className="table-row manager-row" key={member.employeeId}><div className="member-cell"><Avatar name={member.name} /><span><strong>{member.name}</strong><small>{member.email}</small></span></div><span>{member.skillsTracked}</span><div className="readiness-cell"><Progress value={Number(member.readiness)} /><strong>{member.readiness}%</strong></div><button className="text-button" onClick={() => navigate(`/gap-analysis?employeeId=${member.employeeId}`)}>View gaps <ArrowRight size={13} /></button></div>)}
          {!team.length && <div className="empty-state"><span><Users size={16} /></span><strong>No team members assigned</strong><p>Ask your workspace admin to link employees to your team.</p></div>}
        </div>
      </div>
      <div className="panel role-panel"><div className="panel-header"><div><h2>Training requests</h2><p>Team members asking for support.</p></div></div>
        <div className="request-list">{(data.openTrainingRequests || []).map(request => <div className="request-row" key={request.id}><span className={`status-dot status-${String(request.status).toLowerCase()}`} /><span className="request-name">{request.employeeName} · {request.skillName}</span><span className="status-pill">{request.status.replaceAll('_', ' ')}</span><strong className="request-priority">{request.priority}</strong></div>)}
          {!data.openTrainingRequests?.length && <EmptyInline text="No open training requests." />}
        </div>
      </div>
    </>
  }

  const requests = data.openRequests || []
  const sessions = data.sessions || []
  return <>
    <PageHeading eyebrow="SUBJECT MATTER EXPERT" title="SME dashboard" description="Support learners, validate skills, and keep sessions moving." action={<button className="button button-primary" onClick={() => navigate('/assessment')}><Target size={15} /> Validate a skill</button>} />
    {error && <div className="auth-error">{error}</div>}
    <section className="stats-grid">
      <StatCard label="Open requests" value={requests.filter(item => item.status === 'OPEN').length} change="Ready to claim" icon={BookOpen} color="violet" sub="learner support" />
      <StatCard label="Your sessions" value={sessions.length} change="Scheduled & done" icon={Clock3} color="blue" sub="all time" />
      <StatCard label="Assessments" value={(data.recentAssessments || []).length} change="Validated" icon={ShieldCheck} color="green" sub="recent scores" />
      <StatCard label="Pending feedback" value={sessions.filter(item => item.status === 'SCHEDULED').length} change="Complete after session" icon={MessageIcon} color="amber" sub="session workflow" />
    </section>
    <div className="dashboard-grid">
      <div className="panel role-panel"><div className="panel-header"><div><h2>Learning requests</h2><p>Claim an open request to work with an employee.</p></div></div>
        <div className="request-list">{requests.map(request => <div className="sme-request" key={request.id}><div className="sme-request-info"><strong>{request.skillName}</strong><small>{request.employeeName} · {request.priority} priority</small><span>{request.reason || 'No note from learner'}</span></div>{request.status === 'OPEN' && <button className="button button-primary small" disabled={pending} onClick={() => runAction(() => api.patch(`/api/training-requests/${request.id}`, { status: 'CLAIMED' }), 'Request claimed. Create a session below.')}>Claim</button>}{request.status === 'CLAIMED' && request.claimedBy === user.id && <Link className="button button-secondary small" to={`/assessment?employeeId=${request.employeeId}`}>Assess learner</Link>}<span className="status-pill">{request.status.replaceAll('_', ' ')}</span></div>)}
          {!requests.length && <EmptyInline text="No learning requests to work on right now." />}
        </div>
        {requests.some(item => item.status === 'CLAIMED' && item.claimedBy === user.id) && <form className="inline-create" onSubmit={event => { event.preventDefault(); runAction(() => api.post('/api/training-sessions', { trainingRequestId: Number(selectedRequest), sessionTitle, scheduledAt: sessionScheduledAt }), 'Training session scheduled.') }}>
          <select value={selectedRequest} onChange={event => setSelectedRequest(event.target.value)} required><option value="">Choose your claimed request</option>{requests.filter(item => item.status === 'CLAIMED' && item.claimedBy === user.id).map(item => <option key={item.id} value={item.id}>{item.employeeName} · {item.skillName}</option>)}</select>
          <input placeholder="Session title" value={sessionTitle} onChange={event => setSessionTitle(event.target.value)} required />
          <input aria-label="Session date and time" type="datetime-local" value={sessionScheduledAt} onChange={event => setSessionScheduledAt(event.target.value)} required />
          <button className="button button-primary" disabled={pending}>Schedule</button>
        </form>}
      </div>
      <div className="panel role-panel"><div className="panel-header"><div><h2>Your training sessions</h2><p>Submit feedback and score the learner when complete.</p></div></div>
        <div className="session-list">{sessions.map(session => <SessionFeedback key={session.id} session={session} onSubmit={input => runAction(() => api.patch(`/api/training-sessions/${session.id}/feedback`, input), 'Session feedback submitted.')} pending={pending} />)}
          {!sessions.length && <EmptyInline text="No sessions assigned yet." />}
        </div>
      </div>
    </div>
  </>
}

function MessageIcon({ size = 17 }) {
  return <Activity size={size} />
}

function SessionFeedback({ session, onSubmit, pending }) {
  const [feedback, setFeedback] = useState(session.feedback || '')
  const [scoreBefore, setScoreBefore] = useState(session.employeeScoreBefore ?? '')
  const [scoreAfter, setScoreAfter] = useState(session.employeeScoreAfter ?? '')
  return <form className="session-row" onSubmit={event => { event.preventDefault(); onSubmit({ feedback, employeeScoreBefore: scoreBefore === '' ? null : Number(scoreBefore), employeeScoreAfter: scoreAfter === '' ? null : Number(scoreAfter) }) }}>
    <div className="session-heading"><span><strong>{session.sessionTitle}</strong><small>{session.employeeName} · {session.skillName}</small></span><span className="status-pill">{session.status}</span></div>
    {session.status !== 'COMPLETED' && <><div className="session-inputs"><label>Before /10<input type="number" min="0" max="10" value={scoreBefore} onChange={event => setScoreBefore(event.target.value)} required /></label><label>After /10<input type="number" min="0" max="10" value={scoreAfter} onChange={event => setScoreAfter(event.target.value)} required /></label></div><textarea placeholder="Learner feedback and next steps" value={feedback} onChange={event => setFeedback(event.target.value)} required /><button className="button button-primary small" disabled={pending}>Complete with feedback</button></>}
    {session.status === 'COMPLETED' && <p className="session-feedback">{session.feedback || 'No feedback provided'}</p>}
  </form>
}

function EmptyInline({ text }) {
  return <div className="empty-inline">{text}</div>
}

function downloadWorkforceReport(rows) {
  const columns = [
    ['name', 'Employee'],
    ['email', 'Email'],
    ['skillsTracked', 'Skills tracked'],
    ['averageProficiency', 'Average proficiency out of 10'],
    ['developmentAreas', 'Development areas'],
  ]
  const csv = [
    columns.map(([, title]) => title),
    ...rows.map(row => columns.map(([key]) => row[key] ?? '')),
  ].map(row => row.map(value => `"${String(value).replaceAll('"', '""')}"`).join(',')).join('\r\n')
  const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }))
  const link = document.createElement('a')
  link.href = url
  link.download = 'skillsync-workforce-readiness.csv'
  link.click()
  URL.revokeObjectURL(url)
}

function StatCard({ label, value, change, icon: Icon, color, sub }) {
  return <div className="stat-card"><div className="stat-top"><span>{label}</span><i className={`stat-icon ${color}`}><Icon size={17} /></i></div><div className="stat-value">{value}</div><div className="stat-foot"><span className={`stat-change ${color}`}><ArrowUpRight size={13} />{change}</span><span>{sub}</span></div></div>
}

function Progress({ value, color = 'violet' }) {
  return <div className="skill-progress"><i className={color} style={{ width: `${value}%` }} /></div>
}

function SkillSymbol({ index }) {
  const symbols = [Code2, FileText, Zap, BrainCircuit, Activity, ShieldCheck]
  const Icon = symbols[((index % symbols.length) + symbols.length) % symbols.length]
  return <Icon size={16} />
}

function SkillsPage({ setNotice }) {
  const [filter, setFilter] = useState('All skills')
  const [skills, setSkills] = useState([])
  const [catalog, setCatalog] = useState([])
  const [selectedSkill, setSelectedSkill] = useState('')
  const [proficiency, setProficiency] = useState(5)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [refreshKey, setRefreshKey] = useState(0)
  async function load() {
    setLoading(true)
    setError('')
    try {
      const [mappings, allSkills] = await Promise.all([api.get('/api/employee-skills'), api.get('/api/skills')])
      setSkills(mappings.data.map(toUiSkill))
      setCatalog(allSkills.data)
    } catch (requestError) {
      setError(apiErrorMessage(requestError))
    } finally {
      setLoading(false)
    }
  }
  useEffect(() => { load() }, [refreshKey])
  const categories = ['All skills', ...new Set(skills.map(skill => skill.category))]
  const visible = filter === 'All skills' ? skills : skills.filter(skill => skill.category === filter)
  const unmapped = catalog.filter(skill => !skills.some(mapped => mapped.id === skill.id))
  async function addSkill(event) {
    event.preventDefault()
    setSaving(true)
    setError('')
    try {
      await api.post('/api/employee-skills', { skillId: Number(selectedSkill), proficiency: Number(proficiency), yearsOfExperience: 0 })
      setSelectedSkill('')
      setRefreshKey(value => value + 1)
      setNotice('Skill added to your profile.')
    } catch (requestError) {
      setError(apiErrorMessage(requestError))
    } finally {
      setSaving(false)
    }
  }
  async function removeSkill(skill) {
    setError('')
    try {
      await api.delete(`/api/employee-skills/${skill.mappingId}`)
      setRefreshKey(value => value + 1)
      setNotice(`${skill.name} removed from your skills.`)
    } catch (requestError) {
      setError(apiErrorMessage(requestError))
    }
  }
  return <>
    <PageHeading eyebrow="YOUR DEVELOPMENT" title="My skills" description="A clear view of what you know and where you're headed." action={<Link className="button button-primary" to="/assessment"><Target size={16} /> Assess my skills</Link>} />
    {error && <div className="auth-error">{error}<button className="button button-secondary" onClick={() => setRefreshKey(value => value + 1)}>Retry</button></div>}
    <div className="skills-summary-row"><div className="skills-summary-card"><span className="summary-icon violet"><Zap size={17} /></span><strong>{skills.length}</strong><small>Skills tracked</small></div><div className="skills-summary-card"><span className="summary-icon green"><TrendingUp size={17} /></span><strong>{skills.filter(skill => skill.level >= 70).length}</strong><small>Strong skills</small></div><div className="skills-summary-card"><span className="summary-icon amber"><Target size={17} /></span><strong>{skills.filter(skill => skill.level < 60).length}</strong><small>To focus on</small></div></div>
    {unmapped.length > 0 && <form className="inline-create add-skill-form" onSubmit={addSkill}><select value={selectedSkill} onChange={event => setSelectedSkill(event.target.value)} required><option value="">Choose a skill to track</option>{unmapped.map(skill => <option key={skill.id} value={skill.id}>{skill.name}{skill.category ? ` · ${skill.category}` : ''}</option>)}</select><label>Current level <select value={proficiency} onChange={event => setProficiency(event.target.value)}>{Array.from({ length: 11 }, (_, value) => <option value={value} key={value}>{value}/10</option>)}</select></label><button className="button button-primary" disabled={saving}>{saving ? 'Adding…' : 'Add skill'}</button></form>}
    <LoadState loading={loading} error={error && !skills.length ? error : ''} retry={() => setRefreshKey(value => value + 1)} empty={!skills.length}>
      {!!skills.length && <><div className="filter-row">{categories.map(category => <button key={category} className={`filter-chip ${filter === category ? 'selected' : ''}`} onClick={() => setFilter(category)}>{category}</button>)}</div>
      <div className="skills-grid">{visible.map((skill, index) => <div className="panel skill-card" key={skill.id}><div className="skill-card-head"><span className={`focus-icon ${skill.color}`}><SkillSymbol index={index} /></span><button className="text-danger" onClick={() => removeSkill(skill)}>Remove</button></div><div className="skill-category">{skill.category}</div><h3>{skill.name}</h3><div className="skill-level-row"><span>Current proficiency</span><strong>{skill.level}%</strong></div><Progress value={skill.level} color={skill.color} /><div className="skill-card-foot"><span>Current profile rating</span><span className="positive">{skill.level / 10}/10</span></div></div>)}</div></>}
    </LoadState>
  </>
}

function AssessmentPage({ setNotice }) {
  const [searchParams] = useSearchParams()
  const employeeId = searchParams.get('employeeId')
  const [skills, setSkills] = useState([])
  const [ratings, setRatings] = useState({})
  const [submitted, setSubmitted] = useState(false)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [refreshKey, setRefreshKey] = useState(0)
  useEffect(() => {
    let active = true
    Promise.all([
      api.get('/api/skills'),
      api.get('/api/employee-skills', { params: employeeId ? { employeeId } : {} }),
    ])
      .then(([catalog, mappings]) => {
        if (!active) return
        const mapped = new Map(mappings.data.map(item => [item.skillId, item]))
        const list = catalog.data.map((skill, index) => toUiSkill({
          ...skill,
          skillId: skill.id,
          proficiency: mapped.get(skill.id)?.proficiency ?? 0,
        }, index))
        setSkills(list)
        setRatings(Object.fromEntries(list.map(skill => [skill.id, skill.level])))
      })
      .catch(requestError => active && setError(apiErrorMessage(requestError)))
      .finally(() => active && setLoading(false))
    return () => { active = false }
  }, [employeeId, refreshKey])
  async function submitAssessment(event) {
    event.preventDefault()
    setSaving(true)
    setError('')
    try {
      await Promise.all(skills.map(skill => api.post('/api/assessments', {
        ...(employeeId ? { employeeId: Number(employeeId) } : {}),
        skillId: skill.id,
        score: Math.round((ratings[skill.id] || 0) / 10),
      })))
      setSubmitted(true)
      setNotice('Assessment saved. Your skill levels are up to date.')
    } catch (requestError) {
      setError(apiErrorMessage(requestError))
    } finally {
      setSaving(false)
    }
  }
  return <>
    <PageHeading eyebrow="KNOW YOUR STRENGTHS" title="Skill assessment" description="Rate your current confidence. Be honest — this is your space to grow." />
    {error && <div className="auth-error">{error}<button className="button button-secondary" onClick={() => setRefreshKey(value => value + 1)}>Retry</button></div>}
    {submitted && <div className="success-banner"><span><Check size={16} /></span><div><strong>Assessment complete</strong><small>Your dashboard and skill gap analysis have been updated.</small></div><button onClick={() => setSubmitted(false)} aria-label="Dismiss"><X size={16} /></button></div>}
    <LoadState loading={loading} error={error && !skills.length ? error : ''} retry={() => setRefreshKey(value => value + 1)} empty={!skills.length}>
      {!!skills.length && <form className="panel assessment-panel" onSubmit={submitAssessment}><div className="assessment-intro"><div className="assessment-icon"><Target size={20} /></div><div><h2>Rate your confidence</h2><p>Move each slider to match where you are today. Each answer updates your skill profile.</p></div><span className="assessment-step">01 <i>/ 01</i></span></div>
        <div className="rating-list">{skills.map((skill, index) => <div className="rating-item" key={skill.id}><span className={`focus-icon ${skill.color}`}><SkillSymbol index={index} /></span><div className="rating-copy"><strong>{skill.name}</strong><small>{skill.category}</small></div><div className="range-wrap"><input aria-label={`${skill.name} confidence`} type="range" min="0" max="100" value={ratings[skill.id] ?? 0} style={{ '--range': `${ratings[skill.id] ?? 0}%` }} onChange={event => setRatings({ ...ratings, [skill.id]: Number(event.target.value) })} /><div className="range-labels"><span>Just starting</span><span>Confident</span><span>Expert</span></div></div><strong className="range-value">{ratings[skill.id] ?? 0}%</strong></div>)}</div>
        <div className="assessment-actions"><span><ShieldCheck size={15} /> Your answers are private to you</span><button className="button button-primary" type="submit" disabled={saving}>{saving ? 'Saving…' : 'Submit assessment'} <ArrowRight size={15} /></button></div>
      </form>}
    </LoadState>
  </>
}

function GapPage() {
  const [searchParams] = useSearchParams()
  const employeeId = searchParams.get('employeeId')
  const [roles, setRoles] = useState([])
  const [roleId, setRoleId] = useState('')
  const [gaps, setGaps] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [refreshKey, setRefreshKey] = useState(0)
  useEffect(() => {
    let active = true
    api.get('/api/job-roles')
      .then(({ data }) => {
        if (!active) return
        setRoles(data)
        setRoleId(current => current || (data.length ? String(data[0].id) : ''))
      })
      .catch(requestError => active && setError(apiErrorMessage(requestError)))
    return () => { active = false }
  }, [])
  useEffect(() => {
    let active = true
    setLoading(true)
    const params = {
      ...(roleId ? { jobRoleId: roleId } : {}),
      ...(employeeId ? { employeeId } : {}),
    }
    api.get('/api/analytics/skill-gaps', { params })
      .then(({ data }) => active && setGaps(data.map((item, index) => ({
        ...item,
        id: item.skillId,
        name: item.skillName,
        level: Number(item.currentProficiency) * 10,
        target: Number(item.requiredProficiency) * 10,
        gapPoints: Number(item.gap) * 10,
        color: ['violet', 'blue', 'green', 'pink', 'amber', 'cyan'][index % 6],
        category: item.category || 'General',
      }))))
      .catch(requestError => active && setError(apiErrorMessage(requestError)))
      .finally(() => active && setLoading(false))
    return () => { active = false }
  }, [employeeId, roleId, refreshKey])
  const priorityGap = gaps.find(skill => skill.gapPoints > 0)
  return <>
    <PageHeading eyebrow="YOUR NEXT MOVE" title="Skill gap analysis" description="See how your current strengths line up with your next career step." action={<div className="heading-actions">{roles.length > 0 && <select className="select-button" aria-label="Career pathway" value={roleId} onChange={event => setRoleId(event.target.value)}>{roles.map(role => <option value={role.id} key={role.id}>{role.name}</option>)}</select>}<button className="button button-secondary" onClick={() => window.print()}><FileText size={16} /> Export report</button></div>} />
    {error && <div className="auth-error">{error}<button className="button button-secondary" onClick={() => setRefreshKey(value => value + 1)}>Retry</button></div>}
    <LoadState loading={loading} error={error && !gaps.length ? error : ''} retry={() => setRefreshKey(value => value + 1)} empty={!gaps.length}>
      {!!gaps.length && <>
        <div className="gap-highlight"><div className="gap-highlight-icon"><Target size={20} /></div><div><span>YOUR NEXT BEST OPPORTUNITY</span><h2>{priorityGap ? `Build your ${priorityGap.name} proficiency` : 'You are meeting your current targets'}</h2><p>{priorityGap ? `Close a ${priorityGap.gapPoints}-point gap toward your selected career pathway.` : 'Your proficiencies currently match or exceed the selected role requirements.'}</p></div>{priorityGap && <div className="gap-highlight-score"><strong>+{priorityGap.gapPoints}%</strong><small>growth opportunity</small></div>}</div>
        <div className="panel gap-panel"><div className="panel-header"><div><h2>Current vs. target proficiency</h2><p>{roles.find(role => String(role.id) === roleId)?.name || 'General proficiency'}</p></div><span className="comparison-legend"><i className="legend-purple" /> Current <i className="legend-target" /> Target</span></div>
          <div className="gap-list">{gaps.map((skill, index) => <div className="gap-row" key={skill.id}><div className="gap-skill-name"><span className={`focus-icon ${skill.color}`}><SkillSymbol index={index} /></span><div><strong>{skill.name}</strong><small>{skill.category}</small></div></div><div className="gap-bars"><div className="gap-bar-line"><i style={{ width: `${skill.target}%` }} /><b style={{ left: `${skill.level}%` }} /></div><div className="gap-labels"><span>Current <strong>{skill.level}%</strong></span><span>Target <strong>{skill.target}%</strong></span></div></div><span className={`gap-delta ${skill.gapPoints <= 15 ? 'small-gap' : ''}`}>{skill.gapPoints > 0 ? `+${skill.gapPoints}%` : 'Met'}</span>{skill.gapPoints > 0 ? <Link to="/learning" className="icon-button gap-go" aria-label={`Find learning for ${skill.name}`}><ArrowUpRight size={17} /></Link> : <span />}</div>)}</div>
        </div>
      </>}
    </LoadState>
  </>
}

function LearningPage({ setNotice }) {
  const [tab, setTab] = useState('For you')
  const [saved, setSaved] = useState([])
  const [recommendations, setRecommendations] = useState([])
  const [requests, setRequests] = useState([])
  const [sessions, setSessions] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [refreshKey, setRefreshKey] = useState(0)
  const [requesting, setRequesting] = useState(null)
  useEffect(() => {
    let active = true
    setLoading(true)
    Promise.all([
      api.get('/api/learning/recommendations'),
      api.get('/api/training-requests'),
      api.get('/api/training-sessions'),
    ]).then(([recommended, trainingRequests, trainingSessions]) => {
      if (!active) return
      setRecommendations(recommended.data)
      setRequests(trainingRequests.data)
      setSessions(trainingSessions.data)
    }).catch(requestError => active && setError(apiErrorMessage(requestError)))
      .finally(() => active && setLoading(false))
    return () => { active = false }
  }, [refreshKey])
  async function requestTraining(recommendation) {
    setRequesting(recommendation.skillId)
    setError('')
    try {
      await api.post('/api/training-requests', {
        skillId: recommendation.skillId,
        requestedLevel: recommendation.requiredProficiency,
        reason: `Personalized recommendation: ${recommendation.title}`,
        priority: recommendation.gap >= 5 ? 'HIGH' : 'MEDIUM',
      })
      setNotice('Training request sent to subject matter experts.')
      setRefreshKey(value => value + 1)
      setTab('My requests')
    } catch (requestError) {
      setError(apiErrorMessage(requestError))
    } finally {
      setRequesting(null)
    }
  }
  return <>
    <PageHeading eyebrow="GROW AT YOUR PACE" title="Learning hub" description="Curated courses and bite-sized lessons, picked for your goals." action={<button className="button button-secondary" onClick={() => setNotice('Your learning plan is already personalized for you.')}><BrainCircuit size={16} /> Your learning plan</button>} />
    <div className="learning-hero"><div className="learning-hero-copy"><span className="learning-hero-tag"><BookOpen size={13} /> PERSONALIZED FOR YOU</span><h2>Small steps.<br /><span>Big momentum.</span></h2><p>Your next opportunity is just one lesson away. Keep building skills that move your career forward.</p><Link to="/gap-analysis" className="button button-light">Explore your skill gaps <ArrowRight size={15} /></Link></div><div className="learning-orbit"><div className="orbit-ring ring-one" /><div className="orbit-ring ring-two" /><div className="orbit-dot dot-one" /><div className="orbit-dot dot-two" /><div className="orbit-center"><GraduationCap size={35} /></div><div className="orbit-label label-top"><Zap size={13} /> Learn</div><div className="orbit-label label-bottom"><TrendingUp size={13} /> Grow</div></div></div>
    {error && <div className="auth-error">{error}<button className="button button-secondary" onClick={() => setRefreshKey(value => value + 1)}>Retry</button></div>}
    <div className="learning-section-head"><div><h2>{tab === 'For you' ? 'Made for your next step' : tab}</h2><p>{tab === 'For you' ? 'Recommendations generated from your current proficiency gaps' : 'Follow the status of your learning support'}</p></div><div className="filter-row compact">{['For you', 'My requests', 'Sessions'].map(item => <button className={`filter-chip ${tab === item ? 'selected' : ''}`} key={item} onClick={() => setTab(item)}>{item}</button>)}</div></div>
    <LoadState loading={loading} error={error && !recommendations.length && !requests.length ? error : ''} retry={() => setRefreshKey(value => value + 1)} empty={tab === 'For you' ? !recommendations.length : tab === 'My requests' ? !requests.length : !sessions.length}>
      {tab === 'For you' && <div className="course-grid">{recommendations.map((course, index) => <div className="panel course-card" key={`${course.skillId}-${index}`}><div className={`course-cover ${['violet', 'pink', 'amber'][index % 3]}`}><span className="course-cover-icon"><GraduationCap size={24} /></span><span className="course-duration"><Clock3 size={12} /> {course.estimatedHours}h guide</span><div className="cover-decoration" /></div><div className="course-card-body"><div className="course-metadata"><span>{course.skillName}</span><i />{course.gap} point gap</div><h3>{course.title}</h3><p>Guided practice to progress from {course.currentProficiency}/10 toward your {course.requiredProficiency}/10 target.</p><div className="course-card-actions"><button className="button button-primary small" disabled={requesting === course.skillId} onClick={() => requestTraining(course)}>{requesting === course.skillId ? 'Sending…' : 'Request expert help'} <ArrowRight size={14} /></button><button className={`save-course ${saved.includes(course.skillId) ? 'saved' : ''}`} aria-label="Save recommendation" onClick={() => setSaved(value => value.includes(course.skillId) ? value.filter(id => id !== course.skillId) : [...value, course.skillId])}><Check size={15} /></button></div></div></div>)}</div>}
      {tab === 'My requests' && <div className="panel role-panel"><div className="request-list">{requests.map(request => <div className="request-row" key={request.id}><span className={`status-dot status-${String(request.status).toLowerCase()}`} /><span className="request-name">{request.skillName}<small>{request.reason || 'Training request'}</small></span><span className="status-pill">{request.status.replaceAll('_', ' ')}</span><strong className="request-priority">{request.priority}</strong></div>)}</div></div>}
      {tab === 'Sessions' && <div className="panel role-panel"><div className="session-list">{sessions.map(session => <div className="session-row" key={session.id}><div className="session-heading"><span><strong>{session.sessionTitle}</strong><small>{session.skillName}{session.scheduledAt ? ` · ${new Date(session.scheduledAt).toLocaleString()}` : ''}</small></span><span className="status-pill">{session.status}</span></div>{session.feedback && <p className="session-feedback">{session.feedback}</p>}</div>)}</div></div>}
    </LoadState>
  </>
}

function AssistantPage() {
  const [query, setQuery] = useState('')
  const [jobRoleId, setJobRoleId] = useState('')
  const [roles, setRoles] = useState([])
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    api.get('/api/job-roles')
      .then(({ data }) => setRoles(data))
      .catch(requestError => setError(apiErrorMessage(requestError)))
  }, [])

  async function analyze(event) {
    event.preventDefault()
    setError('')
    setLoading(true)
    try {
      const { data } = await api.post('/api/assistant/analyze', {
        query,
        ...(jobRoleId ? { jobRoleId: Number(jobRoleId) } : {}),
      })
      setResult(data)
    } catch (requestError) {
      setError(apiErrorMessage(requestError))
    } finally {
      setLoading(false)
    }
  }

  const examples = [
    'Am I ready for a Java Backend Developer role?',
    'Create a learning roadmap from my skill gaps',
    'What do my recent assessments say?',
  ]

  return <>
    <PageHeading eyebrow="YOUR PERSONAL AI CAREER COACH" title="AI assistant" description="Grounded career, skill-gap and learning guidance from your SkillSync profile." />
    <section className="assistant-hero panel">
      <div className="assistant-hero-icon"><BrainCircuit size={21} /></div>
      <div className="assistant-hero-copy"><span>SKILLSYNC AGENT TEAM</span><h2>Turn your next career question into a clear plan.</h2><p>Skill, assessment and role data is read securely through your signed-in account. No profile facts are invented.</p></div>
    </section>
    <form className="panel assistant-prompt" onSubmit={analyze}>
      <label className="field-label" htmlFor="assistant-query">What would you like to work on?</label>
      <textarea id="assistant-query" value={query} onChange={event => setQuery(event.target.value)} placeholder="For example: Am I ready for a Java Backend Developer role?" maxLength={500} required />
      <div className="assistant-prompt-actions">
        <label className="assistant-role-select">Compare with a role
          <select aria-label="Compare with a job role" value={jobRoleId} onChange={event => setJobRoleId(event.target.value)}>
            <option value="">Choose from my question</option>
            {roles.map(role => <option key={role.id} value={role.id}>{role.name}</option>)}
          </select>
        </label>
        <button className="button button-primary" type="submit" disabled={loading || !query.trim()}>{loading ? <><span className="spinner" /> Analyzing…</> : <>Analyze my profile <Send size={14} /></>}</button>
      </div>
      {error && <div className="auth-error" role="alert">{error}</div>}
      <div className="assistant-examples"><span>TRY ASKING</span>{examples.map(example => <button type="button" key={example} onClick={() => setQuery(example)}>{example}</button>)}</div>
    </form>

    {result && <div className="assistant-results" aria-live="polite">
      <section className="assistant-answer panel">
        <div className="assistant-answer-top"><span className="assistant-answer-mark"><BrainCircuit size={17} /></span><span className="assistant-mode">{result.modelEnhanced ? 'AI intent routing' : 'Grounded analysis'}</span><span className="assistant-intent">{String(result.intent).replaceAll('_', ' ')}</span></div>
        <p>{result.answer}</p>
        <small>{result.modelStatus}</small>
      </section>

      {result.roleName && <section className="panel assistant-readiness">
        <div><span className="assistant-section-kicker">ROLE READINESS</span><h2>{result.roleName}</h2><p>Calculated only from your recorded proficiencies and configured role requirements.</p></div>
        <div className="assistant-score"><strong>{result.readinessPercentage}%</strong><span>readiness</span></div>
      </section>}

      <section className="panel assistant-section">
        <div className="assistant-section-heading"><span className="assistant-section-icon violet"><Target size={17} /></span><div><h2>Skill gap analysis</h2><p>Current vs. required proficiency, from your profile and role setup.</p></div></div>
        {result.skillGaps.length ? <div className="assistant-gap-list">{result.skillGaps.map(gap => <div className="assistant-gap-row" key={gap.skillId}><div><strong>{gap.skillName}</strong><small>{gap.importance} priority</small></div><div className="assistant-gap-meter"><span>Current {gap.currentProficiency}/10</span><i><b style={{ width: `${Math.min(100, gap.currentProficiency * 10)}%` }} /></i><span>Required {gap.requiredProficiency}/10</span></div><strong className={gap.gap ? 'assistant-gap-value' : 'assistant-met'}>{gap.gap ? `-${gap.gap}` : 'Met'}</strong></div>)}</div> : <EmptyInline text={result.roleName ? 'No configured skill requirements were found for this role.' : 'Choose a role with configured skill requirements to compare proficiency.'} />}
      </section>

      <section className="panel assistant-section">
        <div className="assistant-section-heading"><span className="assistant-section-icon blue"><TrendingUp size={17} /></span><div><h2>Career recommendations</h2><p>Ranked by match against configured required skills.</p></div></div>
        {result.careerRecommendations.length ? <div className="assistant-career-list">{result.careerRecommendations.map(career => <article className="assistant-career-card" key={career.jobRoleId}><div className="assistant-career-title"><div><strong>{career.roleName}</strong><small>{career.ready ? 'Requirements met' : 'Skills to strengthen'}</small></div><b>{career.readinessPercentage}%</b></div><div className="assistant-career-bar"><i style={{ width: `${career.readinessPercentage}%` }} /></div><p>{career.explanation}</p>{career.skillGaps.length > 0 && <div className="assistant-tags">{career.skillGaps.map(skill => <span key={skill}>{skill}</span>)}</div>}</article>)}</div> : <EmptyInline text="No job roles with skill requirements are available yet." />}
      </section>

      <section className="panel assistant-section">
        <div className="assistant-section-heading"><span className="assistant-section-icon pink"><GraduationCap size={18} /></span><div><h2>Your learning roadmap</h2><p>Start with the largest recorded gap; work through practical steps in order.</p></div></div>
        {result.learningRoadmap.length ? <div className="assistant-roadmap">{result.learningRoadmap.map(step => <article className="assistant-roadmap-step" key={step.order}><span className="roadmap-number">{String(step.order).padStart(2, '0')}</span><div className="roadmap-step-content"><div className="roadmap-step-title"><div><span>STEP {step.order} · {step.estimatedWeeks} {step.estimatedWeeks === 1 ? 'WEEK' : 'WEEKS'}</span><h3>{step.skillName}</h3></div><b>{step.currentProficiency}/10 <ArrowRight size={13} /> {step.targetProficiency}/10</b></div><p>{step.focus}</p><div className="roadmap-detail-grid"><div><strong>Topics to cover</strong><ul>{step.topics.map(topic => <li key={topic}>{topic}</li>)}</ul></div><div><strong>Practical project</strong><p>{step.practicalProject}</p><a href={step.resourceUrl} target="_blank" rel="noreferrer">{step.resourceName} <ArrowUpRight size={12} /></a></div></div></div></article>)}</div> : <EmptyInline text="Your recorded skills currently meet the selected role's configured levels." />}
      </section>

      <section className="panel assistant-section">
        <div className="assistant-section-heading"><span className="assistant-section-icon amber"><FileText size={17} /></span><div><h2>Assessment insights</h2><p>Latest recorded assessment for each skill.</p></div></div>
        {result.assessmentInsights.length ? <div className="assistant-assessment-list">{result.assessmentInsights.map(item => <div className="assistant-assessment-row" key={item.skillName}><span><strong>{item.skillName}</strong><small>{item.assessmentType.replaceAll('_', ' ')}</small></span><b className={item.latestScore < 6 ? 'assistant-gap-value' : 'assistant-met'}>{item.latestScore}/10</b><p>{item.feedback || item.recommendation}</p></div>)}</div> : <EmptyInline text="No assessments have been recorded yet. Take an assessment to receive score-based recommendations." />}
      </section>
      <p className="assistant-sources">Based on: {result.dataSources.join(' · ')}. Suggested study topics are guidance, not stored learning records.</p>
    </div>}
  </>
}

function ProfilePage({ user, setUser, setNotice }) {
  const [form, setForm] = useState({
    name: user.name,
    email: user.email,
    jobTitle: user.jobTitle || '',
    department: user.department || '',
    careerGoal: user.careerGoal || '',
    bio: user.bio || '',
  })
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  async function save(event) {
    event.preventDefault()
    setSaving(true)
    setError('')
    try {
      const { data } = await api.put('/api/users/me', form)
      setUser(data)
      localStorage.setItem('skillsync.user', JSON.stringify(data))
      setForm({
        name: data.name,
        email: data.email,
        jobTitle: data.jobTitle || '',
        department: data.department || '',
        careerGoal: data.careerGoal || '',
        bio: data.bio || '',
      })
      setNotice('Profile saved successfully.')
    } catch (requestError) {
      setError(apiErrorMessage(requestError))
    } finally {
      setSaving(false)
    }
  }
  return <>
    <PageHeading eyebrow="YOUR ACCOUNT" title="Profile" description="Manage your personal details and career preferences." />
    <div className="profile-layout"><div className="panel profile-card"><div className="profile-cover"><div className="profile-avatar-large"><Avatar name={user.name} size="large" /></div></div><div className="profile-card-body"><h2>{user.name}</h2><p>{user.jobTitle || 'Add your job title'} · {user.role}</p><span className="profile-since"><ShieldCheck size={14} /> SkillSync member</span><div className="profile-divider" /><div className="profile-metric"><span>Department</span><strong>{user.department || 'Not set'}</strong></div><div className="profile-metric"><span>Career goal</span><strong>{user.careerGoal || 'Not set'}</strong></div></div></div>
      <form className="panel profile-form" onSubmit={save}><div className="panel-header"><div><h2>Personal information</h2><p>Update your details and keep your profile current.</p></div><Settings2 size={18} className="muted-icon" /></div>
        {error && <div className="auth-error">{error}</div>}
        <label className="field-label">Full name<input value={form.name} onChange={event => setForm({ ...form, name: event.target.value })} required /></label>
        <label className="field-label">Email address<input type="email" value={form.email} onChange={event => setForm({ ...form, email: event.target.value })} required /></label>
        <label className="field-label">Job title<input value={form.jobTitle} placeholder="e.g. Product designer" onChange={event => setForm({ ...form, jobTitle: event.target.value })} /></label>
        <label className="field-label">Department<input value={form.department} placeholder="e.g. Product" onChange={event => setForm({ ...form, department: event.target.value })} /></label>
        <label className="field-label">Career goal<input value={form.careerGoal} placeholder="e.g. Senior Product Designer" onChange={event => setForm({ ...form, careerGoal: event.target.value })} /></label>
        <label className="field-label">About you<textarea value={form.bio} placeholder="A short introduction" onChange={event => setForm({ ...form, bio: event.target.value })} /></label>
        <div className="form-footer"><span>Role: {user.role} · managed by your admin</span><button className="button button-primary" type="submit" disabled={saving}>{saving ? 'Saving…' : 'Save changes'} <ArrowRight size={14} /></button></div></form></div>
  </>
}

function AuthPage({ mode, onSubmit, notice }) {
  const isRegister = mode === 'register'
  const [form, setForm] = useState({ name: '', email: '', password: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  async function submit(event) {
    event.preventDefault()
    setError('')
    setLoading(true)
    try {
      await onSubmit(isRegister ? form : { email: form.email, password: form.password })
    } catch (requestError) {
      setError(apiErrorMessage(requestError))
    } finally {
      setLoading(false)
    }
  }
  return <div className="auth-page">
    <div className="auth-top"><Link to="/dashboard" className="brand"><span className="brand-mark"><Activity size={19} strokeWidth={2.5} /></span><span>Skill<span className="brand-light">Sync</span></span></Link><span>{isRegister ? 'Already have an account?' : 'New to SkillSync?'} <Link to={isRegister ? '/login' : '/register'}>{isRegister ? 'Sign in' : 'Create an account'} <ArrowRight size={14} /></Link></span></div>
    <div className="auth-main">
      <div className="auth-story"><div className="auth-glow" /><span className="eyebrow"><Activity size={13} /> YOUR CAREER, IN SYNC</span><h1>Make your next<br />move <span>count.</span></h1><p>See your skills clearly. Close the gaps confidently. Build a career that keeps growing.</p><div className="auth-proof"><div className="proof-avatars"><Avatar name="Jules" /><Avatar name="Sam" /><Avatar name="Ari" /></div><span><strong>Join 2,400+</strong><br />professionals growing with intent</span></div><div className="auth-story-foot"><div><Target size={16} /><span>Know your strengths</span></div><div><TrendingUp size={16} /><span>See what's next</span></div><div><GraduationCap size={16} /><span>Keep moving forward</span></div></div></div>
      <div className="auth-form-wrap"><form className="auth-form" onSubmit={submit}><span className="auth-form-kicker">{isRegister ? 'GET STARTED — IT’S FREE' : 'WELCOME BACK'}</span><h2>{isRegister ? 'Create your account' : 'Sign in to SkillSync'}</h2><p>{isRegister ? 'Start building a clearer path forward.' : 'Your next chapter is waiting.'}</p>
        {notice && <div className="auth-notice"><Check size={15} />{notice}</div>}
        {error && <div className="auth-error">{error}</div>}
        {isRegister && <label className="field-label">Full name<div className="auth-input"><UserRound size={16} /><input autoComplete="name" placeholder="Alex Morgan" value={form.name} onChange={event => setForm({ ...form, name: event.target.value })} required /></div></label>}
        <label className="field-label">Email address<div className="auth-input"><Search size={16} /><input type="email" autoComplete="email" placeholder="you@example.com" value={form.email} onChange={event => setForm({ ...form, email: event.target.value })} required /></div></label>
        <label className="field-label">Password<div className="auth-input"><LockKeyhole size={16} /><input type="password" autoComplete={isRegister ? 'new-password' : 'current-password'} placeholder={isRegister ? 'At least 8 characters' : 'Your password'} minLength={isRegister ? 8 : 1} value={form.password} onChange={event => setForm({ ...form, password: event.target.value })} required /></div></label>
        {isRegister && <p className="role-note"><ShieldCheck size={13} /> New accounts start as Employee. A workspace admin can assign other roles.</p>}
        {!isRegister && <div className="auth-form-extras"><label><input type="checkbox" /> Keep me signed in</label><button type="button" onClick={() => setError('For a password reset, please contact your workspace administrator.')}>Forgot password?</button></div>}
        <button className="button button-primary auth-submit" type="submit" disabled={loading}>{loading ? <span className="spinner" /> : isRegister ? 'Create account' : 'Sign in'} {!loading && <ArrowRight size={16} />}</button>
        <div className="auth-secure"><LockKeyhole size={13} /> Your information is encrypted and secure</div>
      </form><div className="auth-legal">By continuing, you agree to SkillSync’s <a href="#terms">Terms of Service</a> and <a href="#privacy">Privacy Policy</a>.</div></div>
    </div>
  </div>
}

export default App
