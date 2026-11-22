import { useState, type FormEvent } from 'react'
import { Button } from '@/components/ui/button'
import { api, setToken } from '@/lib/api'

type Mode = 'login' | 'signup' | 'forgot'

export function AuthScreen({ onEnter }: { onEnter: () => void }) {
  const [mode, setMode] = useState<Mode>('login')
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [error, setError] = useState('')
  const [note, setNote] = useState('')
  const [busy, setBusy] = useState(false)

  async function submit(event: FormEvent) {
    event.preventDefault()
    setError('')
    setNote('')
    setBusy(true)
    try {
      if (mode === 'signup') {
        if (password.length < 6) throw new Error('Use at least 6 characters.')
        await api.signup({ name, email, password })
        const token = await api.login({ email, password })
        setToken(token)
        onEnter()
      } else if (mode === 'login') {
        const token = await api.login({ email, password })
        setToken(token)
        onEnter()
      } else {
        if (password !== confirm) throw new Error('The two passwords do not match.')
        await api.forgot({ email, newPassword: password })
        setNote('Password updated. Sign in with the new one.')
        setMode('login')
        setPassword('')
        setConfirm('')
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Something went wrong.')
    } finally {
      setBusy(false)
    }
  }

  const title = mode === 'signup' ? 'Create an account' : mode === 'forgot' ? 'Set a new password' : 'Sign in'

  return (
    <div className="grid min-h-svh lg:grid-cols-2">
      <section className="relative hidden min-h-svh overflow-hidden bg-panel text-panel-foreground lg:block">
        <div className="absolute top-16 left-16 size-56 rounded-full border border-panel-foreground/30" />
        <div className="absolute top-28 left-28 size-32 rounded-full border border-panel-foreground/50" />
        <div className="relative flex h-full flex-col justify-end p-14">
          <p className="text-6xl font-medium tracking-tight">Circlo</p>
          <p className="mt-4 max-w-sm text-lg text-panel-foreground/80">
            Publish work to the people you actually know.
          </p>
        </div>
      </section>

      <section className="flex items-center justify-center px-6 py-16">
        <div className="w-full max-w-sm">
          <p className="mb-8 text-2xl font-medium lg:hidden">Circlo</p>
          <h1 className="text-2xl font-medium">{title}</h1>
          <p className="mt-2 text-sm text-muted-foreground">
            {mode === 'forgot'
              ? 'This stack has no mail service, so the new password is saved now.'
              : 'Use the account you created on this machine.'}
          </p>

          <form className="mt-8 space-y-4" onSubmit={submit}>
            {mode === 'signup' && (
              <Field label="Name" value={name} onChange={setName} autoComplete="name" />
            )}
            <Field label="Email" type="email" value={email} onChange={setEmail} autoComplete="email" />
            <Field
              label={mode === 'forgot' ? 'New password' : 'Password'}
              type="password"
              value={password}
              onChange={setPassword}
              autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
            />
            {mode === 'forgot' && (
              <Field label="Confirm password" type="password" value={confirm} onChange={setConfirm} autoComplete="new-password" />
            )}
            {error && <p className="text-sm text-destructive">{error}</p>}
            {note && <p className="text-sm text-foreground">{note}</p>}
            <Button type="submit" className="w-full" disabled={busy}>
              {busy ? 'Working' : title}
            </Button>
          </form>

          <div className="mt-6 flex flex-wrap gap-x-4 gap-y-2 text-sm">
            {mode !== 'login' && (
              <button type="button" className="underline-offset-4 hover:underline active:opacity-80 disabled:opacity-50" onClick={() => setMode('login')}>
                Sign in
              </button>
            )}
            {mode !== 'signup' && (
              <button type="button" className="underline-offset-4 hover:underline active:opacity-80 disabled:opacity-50" onClick={() => setMode('signup')}>
                Create an account
              </button>
            )}
            {mode !== 'forgot' && (
              <button type="button" className="underline-offset-4 hover:underline active:opacity-80 disabled:opacity-50" onClick={() => setMode('forgot')}>
                Forgot password
              </button>
            )}
          </div>
        </div>
      </section>
    </div>
  )
}

function Field({
  label,
  value,
  onChange,
  type = 'text',
  autoComplete,
}: {
  label: string
  value: string
  onChange: (value: string) => void
  type?: string
  autoComplete?: string
}) {
  const id = label.toLowerCase().replace(/\s+/g, '-')
  return (
    <label className="block text-sm" htmlFor={id}>
      {label}
      <input
        id={id}
        className="mt-1 h-10 w-full rounded-md border border-input bg-card px-3 text-base"
        type={type}
        value={value}
        autoComplete={autoComplete}
        required
        onChange={(event) => onChange(event.target.value)}
      />
    </label>
  )
}
