#!/usr/bin/env python3
import asyncio
import json
import threading
import email as email_lib
from http.server import HTTPServer, BaseHTTPRequestHandler
from aiosmtpd.controller import Controller
from datetime import datetime

emails = []

HTML_TEMPLATE = """<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<meta http-equiv="refresh" content="5">
<title>Mail Server</title>
<style>
  body { font-family: monospace; background: #1a1a1a; color: #eee; padding: 20px; }
  h1 { color: #4fc3f7; }
  .email { background: #2a2a2a; border-left: 4px solid #4fc3f7; padding: 12px; margin: 10px 0; border-radius: 4px; }
  .meta { color: #aaa; font-size: 0.85em; }
  .subject { color: #fff; font-weight: bold; font-size: 1.1em; }
  .body { background: #111; padding: 10px; margin-top: 8px; white-space: pre-wrap; color: #b2ff59; }
  .empty { color: #888; }
</style>
</head>
<body>
<h1>📬 Mail Server — SubastaApp</h1>
<p class="meta">Auto-refresh cada 5s | {count} email(s) recibido(s)</p>
{content}
</body>
</html>"""

EMAIL_ITEM = """<div class="email">
  <div class="meta">{time} &nbsp;|&nbsp; De: {from_} &nbsp;→&nbsp; Para: {to}</div>
  <div class="subject">{subject}</div>
  <div class="body">{body}</div>
</div>"""


class SMTPHandler:
    async def handle_DATA(self, server, session, envelope):
        msg = email_lib.message_from_bytes(envelope.content)
        body = _extract_body(msg)
        entry = {
            "id": len(emails) + 1,
            "from": envelope.mail_from,
            "to": envelope.rcpt_tos,
            "subject": msg.get("Subject", "(sin asunto)"),
            "body": body,
            "time": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
        }
        emails.append(entry)
        print(f"[MAIL] Para: {entry['to']} | Asunto: {entry['subject']}")
        print(f"[MAIL] {body[:300]}")
        return "250 OK"


def _extract_body(msg):
    if msg.is_multipart():
        for part in msg.walk():
            if part.get_content_type() == "text/plain":
                payload = part.get_payload(decode=True)
                return payload.decode("utf-8", errors="replace") if payload else ""
    payload = msg.get_payload(decode=True)
    if payload:
        return payload.decode("utf-8", errors="replace")
    return str(msg.get_payload())


class HTTPHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == "/api/emails":
            data = json.dumps(list(reversed(emails)), ensure_ascii=False)
            self._respond(200, "application/json", data.encode())
        else:
            items = "".join(
                EMAIL_ITEM.format(
                    time=e["time"],
                    from_=e["from"],
                    to=", ".join(e["to"]),
                    subject=e["subject"],
                    body=e["body"],
                )
                for e in reversed(emails)
            ) or '<p class="empty">No hay emails todavia. Refrescando...</p>'
            html = HTML_TEMPLATE.format(count=len(emails), content=items)
            self._respond(200, "text/html; charset=utf-8", html.encode())

    def _respond(self, code, ctype, body):
        self.send_response(code)
        self.send_header("Content-Type", ctype)
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, fmt, *args):
        pass


def run_http():
    HTTPServer(("0.0.0.0", 8025), HTTPHandler).serve_forever()


async def main():
    controller = Controller(SMTPHandler(), hostname="0.0.0.0", port=1025)
    controller.start()
    print("SMTP escuchando en :1025")
    print("Web UI en http://localhost:8025")
    threading.Thread(target=run_http, daemon=True).start()
    await asyncio.Event().wait()


if __name__ == "__main__":
    asyncio.run(main())
