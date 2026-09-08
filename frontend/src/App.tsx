import { useState } from 'react'
import './App.css'

export default function App() {
  const [status] = useState('시스템 준비 완료')

  return (
    <main className="app-container">
      <header className="app-header">
        <h1>jchess</h1>
        <p className="subtitle">Java 25 &amp; Spring Boot 4 기반 실시간 온라인 체스</p>
      </header>
      <section className="status-card">
        <h2>Frontend 상태</h2>
        <p className="status-text">{status}</p>
        <p className="description">
          Vite + React 19 + TypeScript 개발 환경이 초기화되었습니다.
        </p>
      </section>
    </main>
  )
}