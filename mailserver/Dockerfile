FROM python:3.11-slim
RUN pip install --no-cache-dir aiosmtpd
WORKDIR /app
COPY smtp_server.py .
EXPOSE 1025 8025
CMD ["python", "-u", "smtp_server.py"]
