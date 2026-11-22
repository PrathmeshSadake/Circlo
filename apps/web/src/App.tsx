import { useState } from 'react'
import { AuthScreen } from '@/auth/AuthScreen'
import { Workspace } from '@/app/Workspace'
import { getToken } from '@/lib/api'

export default function App() {
  const [authed, setAuthed] = useState(Boolean(getToken()))
  return authed ? <Workspace onLeave={() => setAuthed(false)} /> : <AuthScreen onEnter={() => setAuthed(true)} />
}
