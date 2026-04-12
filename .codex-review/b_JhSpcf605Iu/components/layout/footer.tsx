import Link from "next/link"

export function Footer() {
  return (
    <footer className="border-t border-border mt-auto">
      <div className="mx-auto max-w-3xl px-6 py-10">
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-6">
          {/* Brand */}
          <div>
            <Link href="/" className="font-serif text-lg font-bold">
              PostForge
            </Link>
            <p className="text-sm text-muted-foreground mt-1">
              AI 기반 한국 주식 분석
            </p>
          </div>

          {/* Links */}
          <div className="flex items-center gap-6 text-sm text-muted-foreground">
            <Link href="#" className="hover:text-foreground transition-colors">
              이용약관
            </Link>
            <Link href="#" className="hover:text-foreground transition-colors">
              개인정보처리방침
            </Link>
            <Link href="#" className="hover:text-foreground transition-colors">
              문의
            </Link>
          </div>
        </div>

        <div className="mt-8 pt-6 border-t border-border text-xs text-muted-foreground">
          <p>
            투자 판단의 책임은 투자자 본인에게 있습니다. 본 서비스는 투자 권유가 아닙니다.
          </p>
          <p className="mt-2">
            &copy; 2026 PostForge
          </p>
        </div>
      </div>
    </footer>
  )
}
