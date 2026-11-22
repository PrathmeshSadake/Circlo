import { Button } from '@/components/ui/button'

const apiUrl = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'

export default function App() {
  return (
    <main className="flex min-h-svh items-center justify-center bg-background text-foreground">
      <div className="space-y-4 text-center">
        <h1 className="text-3xl font-semibold tracking-tight">LinkedIn</h1>
        <p className="text-sm text-muted-foreground">
          Blank frontend. The API gateway is {apiUrl}.
        </p>
        <Button type="button">Get started</Button>
      </div>
    </main>
  )
}
