import { useEffect, useState } from 'react'
import { Button } from '@/components/ui/button'
import { api, getToken, setToken, type Notice, type Person, type Post, type User } from '@/lib/api'

type View = 'feed' | 'network' | 'profile'

function viewFromHash(): View {
  const hash = window.location.hash.replace('#/', '')
  if (hash === 'network' || hash === 'profile') return hash
  return 'feed'
}

export function Workspace({ onLeave }: { onLeave: () => void }) {
  const [view, setView] = useState<View>(viewFromHash)
  const [me, setMe] = useState<User | null>(null)
  const [posts, setPosts] = useState<Post[]>([])
  const [people, setPeople] = useState<User[]>([])
  const [circle, setCircle] = useState<Person[]>([])
  const [notices, setNotices] = useState<Notice[]>([])
  const [liked, setLiked] = useState<number[]>([])
  const [draft, setDraft] = useState('')
  const [mediaUrl, setMediaUrl] = useState('')
  const [status, setStatus] = useState<'loading' | 'ready' | 'error'>('loading')
  const [message, setMessage] = useState('')
  const [showNotices, setShowNotices] = useState(false)

  useEffect(() => {
    const onHash = () => setView(viewFromHash())
    window.addEventListener('hashchange', onHash)
    return () => window.removeEventListener('hashchange', onHash)
  }, [])

  async function load() {
    setStatus('loading')
    setMessage('')
    try {
      const user = await api.me()
      const [feed, directory, notes, friends] = await Promise.all([
        api.feed(),
        api.people(),
        api.notices().catch(() => [] as Notice[]),
        api.connections(user.id).catch(() => [] as Person[]),
      ])
      setMe(user)
      setPosts(feed)
      setPeople(directory)
      setNotices(notes)
      setCircle(friends)
      setStatus('ready')
    } catch (err) {
      if (!getToken()) onLeave()
      setStatus('error')
      setMessage(err instanceof Error ? err.message : 'Could not load Circlo.')
    }
  }

  useEffect(() => {
    load()
  }, [])

  function go(next: View) {
    window.location.hash = `/${next}`
    setView(next)
    setShowNotices(false)
  }

  function leave() {
    setToken(null)
    onLeave()
  }

  async function publish() {
    if (!draft.trim()) return
    setMessage('')
    try {
      const post = await api.createPost({ content: draft.trim(), mediaUrl: mediaUrl || undefined })
      setPosts((current) => [post, ...current])
      setDraft('')
      setMediaUrl('')
    } catch (err) {
      setMessage(err instanceof Error ? err.message : 'Post failed.')
    }
  }

  async function attach(file: File | undefined, usage: 'post' | 'profile') {
    if (!file) return
    setMessage('')
    try {
      const uploaded = await api.upload(file, usage)
      if (usage === 'post') setMediaUrl(uploaded.url)
      else {
        const user = await api.me()
        setMe(user)
      }
    } catch (err) {
      setMessage(err instanceof Error ? err.message : 'Upload failed.')
    }
  }

  async function toggleLike(postId: number) {
    const already = liked.includes(postId)
    try {
      if (already) await api.unlike(postId)
      else await api.like(postId)
      setLiked((current) => (already ? current.filter((id) => id !== postId) : [...current, postId]))
    } catch (err) {
      const text = err instanceof Error ? err.message : ''
      if (text.toLowerCase().includes('already liked')) {
        setLiked((current) => (current.includes(postId) ? current : [...current, postId]))
      } else setMessage(text || 'Like failed.')
    }
  }

  const nameOf = (userId: number) => people.find((person) => person.id === userId)?.name || `User ${userId}`

  return (
    <div className="min-h-svh">
      <header className="border-b border-border bg-card">
        <div className="mx-auto flex h-14 max-w-6xl items-center gap-6 px-4">
          <button type="button" className="text-lg font-medium" onClick={() => go('feed')}>
            Circlo
          </button>
          <nav className="flex gap-4 text-sm">
            <NavButton current={view === 'feed'} onClick={() => go('feed')}>Feed</NavButton>
            <NavButton current={view === 'network'} onClick={() => go('network')}>Network</NavButton>
            <NavButton current={view === 'profile'} onClick={() => go('profile')}>Profile</NavButton>
          </nav>
          <div className="ml-auto flex items-center gap-3">
            <button type="button" className="text-sm" onClick={() => setShowNotices((open) => !open)}>
              Notices {notices.length ? `(${notices.length})` : ''}
            </button>
            <button type="button" className="text-sm text-muted-foreground" onClick={leave}>
              Sign out
            </button>
          </div>
        </div>
      </header>

      {showNotices && (
        <NoticeList
          notices={notices}
          onAccept={async (id) => {
            await api.acceptRequest(id)
            await load()
          }}
          onDecline={async (id) => {
            await api.declineRequest(id)
            await load()
          }}
        />
      )}

      <main className="mx-auto grid max-w-6xl gap-8 px-4 py-8 lg:grid-cols-[16rem_minmax(0,1fr)_18rem]">
        <aside className="hidden lg:block">
          <ProfileBlock user={me} />
        </aside>

        <section className="min-w-0">
          {status === 'loading' && <Skeleton />}
          {status === 'error' && (
            <div className="border border-border bg-card p-6">
              <p>{message}</p>
              <Button className="mt-4" type="button" onClick={load}>Try again</Button>
            </div>
          )}
          {status === 'ready' && view === 'feed' && (
            <Feed
              draft={draft}
              setDraft={setDraft}
              mediaUrl={mediaUrl}
              onFile={(file) => attach(file, 'post')}
              onPublish={publish}
              posts={posts}
              nameOf={nameOf}
              liked={liked}
              onLike={toggleLike}
              message={message}
            />
          )}
          {status === 'ready' && view === 'network' && me && (
            <Network
              me={me}
              people={people}
              circle={circle}
              message={message}
              onSend={async (id) => {
                await api.sendRequest(id)
                setMessage('They have the request.')
              }}
              onCancel={async (id) => {
                await api.cancelRequest(id)
                setMessage('The pending request is gone.')
              }}
              onError={setMessage}
            />
          )}
          {status === 'ready' && view === 'profile' && me && (
            <ProfilePage
              me={me}
              posts={posts.filter((post) => post.userId === me.id)}
              onFile={(file) => attach(file, 'profile')}
              message={message}
            />
          )}
        </section>

        <aside className="hidden lg:block">
          <h2 className="text-sm text-muted-foreground">Notices</h2>
          <NoticeBody notices={notices} compact />
        </aside>
      </main>
    </div>
  )
}

function NavButton({ current, onClick, children }: { current: boolean; onClick: () => void; children: string }) {
  return (
    <button
      type="button"
      className={current ? 'text-foreground' : 'text-muted-foreground'}
      aria-current={current ? 'page' : undefined}
      onClick={onClick}
    >
      {children}
    </button>
  )
}

function ProfileBlock({ user }: { user: User | null }) {
  return (
    <div>
      <Portrait url={user?.profileImageUrl} name={user?.name || 'You'} />
      <p className="mt-3 text-base font-medium">{user?.name || 'Loading'}</p>
      <p className="text-sm text-muted-foreground">{user?.email}</p>
    </div>
  )
}

function Portrait({ url, name }: { url?: string | null; name: string }) {
  if (url) return <img src={url} alt="" className="size-16 object-cover" />
  return (
    <div className="flex size-16 items-center justify-center bg-secondary text-sm">
      {name.slice(0, 1).toUpperCase()}
    </div>
  )
}

function Feed({
  draft,
  setDraft,
  mediaUrl,
  onFile,
  onPublish,
  posts,
  nameOf,
  liked,
  onLike,
  message,
}: {
  draft: string
  setDraft: (value: string) => void
  mediaUrl: string
  onFile: (file: File | undefined) => void
  onPublish: () => void
  posts: Post[]
  nameOf: (id: number) => string
  liked: number[]
  onLike: (id: number) => void
  message: string
}) {
  return (
    <div>
      <h1 className="text-xl font-medium">Feed</h1>
      <form
        className="mt-4 border border-border bg-card p-4"
        onSubmit={(event) => {
          event.preventDefault()
          onPublish()
        }}
      >
        <label className="text-sm" htmlFor="post">
          Write something
          <textarea
            id="post"
            className="mt-1 min-h-24 w-full resize-y border border-input bg-background p-3 text-base"
            value={draft}
            onChange={(event) => setDraft(event.target.value)}
          />
        </label>
        <div className="mt-3 flex flex-wrap items-center gap-3">
          <label className="text-sm underline-offset-4 hover:underline">
            Add an image
            <input
              className="sr-only"
              type="file"
              accept="image/jpeg,image/png,image/webp,image/gif"
              onChange={(event) => onFile(event.target.files?.[0])}
            />
          </label>
          {mediaUrl && <span className="text-sm text-muted-foreground">Image attached</span>}
          <Button className="ml-auto" type="submit">Post</Button>
        </div>
        {mediaUrl && <img src={mediaUrl} alt="" className="mt-3 max-h-48 w-full object-cover" />}
      </form>
      {message && <p className="mt-3 text-sm text-destructive">{message}</p>}
      {posts.length === 0 ? (
        <p className="mt-8 text-sm text-muted-foreground">No posts yet. Write the first one.</p>
      ) : (
        <ul className="mt-6 divide-y divide-border border-y border-border">
          {posts.map((post) => (
            <li key={post.id} className="bg-card px-4 py-5">
              <p className="text-sm text-muted-foreground">{nameOf(post.userId)}</p>
              <p className="mt-2 whitespace-pre-wrap text-base">{post.content}</p>
              {post.mediaUrl && <img src={post.mediaUrl} alt="" className="mt-3 max-h-80 w-full object-cover" />}
              <button type="button" className="mt-3 text-sm" onClick={() => onLike(post.id)}>
                {liked.includes(post.id) ? 'Liked' : 'Like'}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

function Network({
  me,
  people,
  circle,
  message,
  onSend,
  onCancel,
  onError,
}: {
  me: User
  people: User[]
  circle: Person[]
  message: string
  onSend: (id: number) => Promise<void>
  onCancel: (id: number) => Promise<void>
  onError: (value: string) => void
}) {
  const connected = new Set(circle.map((person) => person.userId))
  const others = people.filter((person) => person.id !== me.id)
  return (
    <div>
      <h1 className="text-xl font-medium">Network</h1>
      <p className="mt-2 text-sm text-muted-foreground">
        {circle.length ? `${circle.length} first-degree ${circle.length === 1 ? 'connection' : 'connections'}` : 'No first-degree connections yet.'}
      </p>
      {message && <p className="mt-3 text-sm">{message}</p>}
      {others.length === 0 ? (
        <p className="mt-8 text-sm text-muted-foreground">Create another account to send a request.</p>
      ) : (
        <ul className="mt-6 divide-y divide-border border-y border-border">
          {others.map((person) => (
            <li key={person.id} className="flex items-center gap-4 bg-card px-4 py-4">
              <div className="min-w-0">
                <p className="font-medium">{person.name}</p>
                <p className="text-sm text-muted-foreground">{person.email}</p>
              </div>
              <div className="ml-auto flex gap-2">
                {connected.has(person.id) ? (
                  <span className="text-sm text-muted-foreground">Connected</span>
                ) : (
                  <>
                    <Button
                      type="button"
                      onClick={() => onSend(person.id).catch((err) => onError(err.message))}
                    >
                      Connect
                    </Button>
                    <Button
                      type="button"
                      variant="outline"
                      onClick={() => onCancel(person.id).catch((err) => onError(err.message))}
                    >
                      Cancel
                    </Button>
                  </>
                )}
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

function ProfilePage({
  me,
  posts,
  onFile,
  message,
}: {
  me: User
  posts: Post[]
  onFile: (file: File | undefined) => void
  message: string
}) {
  return (
    <div>
      <h1 className="text-xl font-medium">Profile</h1>
      <div className="mt-4 flex items-center gap-4">
        <Portrait url={me.profileImageUrl} name={me.name} />
        <div>
          <p className="font-medium">{me.name}</p>
          <p className="text-sm text-muted-foreground">{me.email}</p>
          <label className="mt-2 inline-block text-sm underline-offset-4 hover:underline">
            Upload a profile picture
            <input
              className="sr-only"
              type="file"
              accept="image/jpeg,image/png,image/webp,image/gif"
              onChange={(event) => onFile(event.target.files?.[0])}
            />
          </label>
        </div>
      </div>
      {message && <p className="mt-3 text-sm text-destructive">{message}</p>}
      <h2 className="mt-8 text-base font-medium">Your posts</h2>
      {posts.length === 0 ? (
        <p className="mt-2 text-sm text-muted-foreground">Nothing published yet.</p>
      ) : (
        <ul className="mt-3 divide-y divide-border border-y border-border">
          {posts.map((post) => (
            <li key={post.id} className="bg-card px-4 py-4">
              <p>{post.content}</p>
              {post.mediaUrl && <img src={post.mediaUrl} alt="" className="mt-3 max-h-64 w-full object-cover" />}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

function NoticeList({
  notices,
  onAccept,
  onDecline,
}: {
  notices: Notice[]
  onAccept: (id: number) => Promise<void>
  onDecline: (id: number) => Promise<void>
}) {
  return (
    <div className="border-b border-border bg-card">
      <div className="mx-auto max-w-6xl px-4 py-4">
        <NoticeBody notices={notices} onAccept={onAccept} onDecline={onDecline} />
      </div>
    </div>
  )
}

function NoticeBody({
  notices,
  compact,
  onAccept,
  onDecline,
}: {
  notices: Notice[]
  compact?: boolean
  onAccept?: (id: number) => Promise<void>
  onDecline?: (id: number) => Promise<void>
}) {
  if (!notices.length) return <p className="text-sm text-muted-foreground">No notices yet.</p>
  return (
    <ul className={compact ? 'mt-3 space-y-3' : 'space-y-3'}>
      {notices.map((notice) => (
        <li key={notice.id} className="text-sm">
          <p>{notice.message}</p>
          {notice.type === 'CONNECTION_REQUESTED' && onAccept && onDecline && (
            <div className="mt-2 flex gap-2">
              <Button type="button" onClick={() => onAccept(notice.referenceId)}>Accept</Button>
              <Button type="button" variant="outline" onClick={() => onDecline(notice.referenceId)}>Decline</Button>
            </div>
          )}
        </li>
      ))}
    </ul>
  )
}

function Skeleton() {
  return (
    <div className="space-y-3" aria-hidden="true">
      <div className="h-8 w-32 bg-muted" />
      <div className="h-32 bg-muted" />
      <div className="h-24 bg-muted" />
    </div>
  )
}
