#!/bin/bash
# Master setup script for API log forwarding on Ubuntu with queue rotation
# and automatic named pipe recreation

set -euo pipefail

# ===== Configuration =====
API_URL="https://log.dev.krhscougarband.org/ingest/syslog"
PIPE="/var/log/api_forward/api_pipe"
QUEUE="/var/log/api_forward/queue.log"
SERVICE="/etc/systemd/system/send_log_api.service"
MAX_QUEUE_SIZE=$((10 * 1024 * 1024))  # 10 MB
MAX_ROTATED=5

echo "=== Updating and installing packages ==="
sudo apt update
sudo apt install -y rsyslog curl jq

echo "=== Setting up /var/log/api_forward directory ==="
sudo mkdir -p /var/log/api_forward
sudo chown syslog:adm /var/log/api_forward
sudo chmod 775 /var/log/api_forward

echo "=== Creating named pipe if missing ==="
if [ ! -p "$PIPE" ]; then
    sudo mkfifo -m 660 "$PIPE"
    sudo chown syslog:adm "$PIPE"
fi

echo "=== Creating queue log ==="
sudo touch "$QUEUE"
sudo chown syslog:adm "$QUEUE"
sudo chmod 664 "$QUEUE"

echo "=== Writing send_log_api.sh script with queue rotation, diagnostics, and optional HMAC auth ==="
sudo tee /usr/local/bin/send_log_api.sh > /dev/null <<EOF
#!/bin/bash
set -euo pipefail
PIPE="/var/log/api_forward/api_pipe"
QUEUE="/var/log/api_forward/queue.log"
API_URL="$API_URL"
MAX_RETRIES=5
SLEEP_BETWEEN_RETRIES=5
MAX_QUEUE_SIZE=\$((10 * 1024 * 1024))  # 10 MB
MAX_ROTATED=5

# Optional HMAC auth settings (set via environment in unit or systemd drop-in):
# HMAC_SECRET: shared secret for signing
# HMAC_HEADER: header name for signature (default: X-Signature)
# HMAC_TS_HEADER: header name for timestamp (default: X-Timestamp)
# HMAC_ALGO: sha256 or sha1 (default: sha256)
HMAC_HEADER="\${HMAC_HEADER:-X-Signature}"
HMAC_TS_HEADER="\${HMAC_TS_HEADER:-X-Timestamp}"
HMAC_ALGO="\${HMAC_ALGO:-sha256}"

build_auth_headers() {
    local payload="\$1"
    if [ -n "\${HMAC_SECRET:-}" ]; then
        local ts
        ts=\$(date -u +%s)
        # Signature over timestamp + payload to prevent replay (common pattern)
        local signing_input
        signing_input="\$ts:\$payload"
        local sig
        sig=\$(printf '%s' "\$signing_input" | openssl dgst -\$HMAC_ALGO -hmac "\$HMAC_SECRET" -binary | openssl base64 -A)
        echo "\$HMAC_HEADER: \$sig"
        echo "\$HMAC_TS_HEADER: \$ts"
    fi
}

rotate_queue() {
    if [ -f "\$QUEUE" ] && [ \$(stat -c%s "\$QUEUE") -ge \$MAX_QUEUE_SIZE ]; then
        # Remove oldest if at cap
        if [ -f "\$QUEUE.\$MAX_ROTATED" ]; then
            rm -f "\$QUEUE.\$MAX_ROTATED"
        fi
        # Shift older rotations upward: .(n-1) -> .n
        for ((i=MAX_ROTATED; i>=2; i--)); do
            if [ -f "\$QUEUE.\$((i-1))" ]; then
                mv "\$QUEUE.\$((i-1))" "\$QUEUE.\$i"
            fi
        done
        mv "\$QUEUE" "\$QUEUE.1"
        : > "\$QUEUE"
        chown syslog:adm "\$QUEUE"
        chmod 664 "\$QUEUE"
    fi
}

check_pipe() {
    if [ ! -p "\$PIPE" ]; then
        mkfifo -m 660 "\$PIPE"
        chown syslog:adm "\$PIPE"
        logger -t api-forward "Named pipe recreated: \$PIPE"
    fi
}

send_log() {
    local line="\$1"
    local attempt=1
    while [ \$attempt -le \$MAX_RETRIES ]; do
        # Rsyslog writes complete JSON lines using ApiForwardJson; forward as-is.
        local payload
        payload="\$line"
        
        # Build headers array (includes optional HMAC)
        local -a headers
        headers=("Content-Type: application/json")
        if auth_headers=\$(build_auth_headers "\$payload"); then
            while IFS= read -r h; do
                headers+=("\$h")
            done <<< "\$auth_headers"
        fi
        
        # Send POST; build curl command array properly
        local http_code http_body curl_exit
        http_body=\$(mktemp)
        local -a curl_cmd
        curl_cmd=(curl -sS -X POST "\$API_URL" -w "%{http_code}" -o "\$http_body" -d "\$payload")
        for hdr in "\${headers[@]}"; do
            curl_cmd+=(-H "\$hdr")
        done
        
        # Run curl and capture exit code (stderr goes to journald)
        set +e
        http_code=\$("\${curl_cmd[@]}")
        curl_exit=\$?
        set -e
        
        # Check if curl itself failed (non-zero exit or non-numeric status)
        if [ \$curl_exit -ne 0 ] || ! [[ "\$http_code" =~ ^[0-9]{3}\$ ]]; then
            logger -t api-forward "curl failed with exit code \$curl_exit"
            rm -f "\$http_body"
            sleep \$SLEEP_BETWEEN_RETRIES
            attempt=\$((attempt+1))
            continue
        fi
        
        # Accept 200, 201, 202 as success
        if [ "\$http_code" = "200" ] || [ "\$http_code" = "201" ] || [ "\$http_code" = "202" ]; then
            logger -t api-forward "Forwarded log successfully (HTTP \$http_code)"
            rm -f "\$http_body"
            return 0
        else
            local body_text
            body_text=\$(cat "\$http_body" 2>/dev/null || echo "")
            logger -t api-forward "Failed to forward log (HTTP \$http_code): \$body_text"
            rm -f "\$http_body"
        fi
        sleep \$SLEEP_BETWEEN_RETRIES
        attempt=\$((attempt+1))
    done
    echo "\$line" >> "\$QUEUE"
    rotate_queue
    return 1
}

while true; do
    check_pipe

    # Retry queued logs first
    if [ -s "\$QUEUE" ]; then
        tmp_queue="\${QUEUE}.tmp"
        : > "\$tmp_queue"
        chown syslog:adm "\$tmp_queue"
        chmod 664 "\$tmp_queue"
        while IFS= read -r queued_line; do
            send_log "\$queued_line" || echo "\$queued_line" >> "\$tmp_queue"
        done < "\$QUEUE"
        mv "\$tmp_queue" "\$QUEUE"
    fi

    # Read new logs from pipe
    if IFS= read -r line <"\$PIPE"; then
        send_log "\$line"
    fi
done
EOF

sudo chmod +x /usr/local/bin/send_log_api.sh
sudo chown syslog:adm /usr/local/bin/send_log_api.sh

echo "=== Creating rsyslog routing to named pipe with JSON template (includes facility/severity) ==="
sudo tee /etc/rsyslog.d/forward_to_pipe.conf > /dev/null <<'EOF'
# JSON template including facility and severity (and host/program/message)
template(name="ApiForwardJson" type="list") {
    constant(value="{")
    constant(value="\"host\":\"")       property(name="hostname" format="json")        constant(value="\",")
    constant(value="\"program\":\"")    property(name="programname" format="json")     constant(value="\",")
    constant(value="\"message\":\"")    property(name="msg" format="json")             constant(value="\",")
    constant(value="\"facility\":")     property(name="syslogfacility")                constant(value=",")
    constant(value="\"severity\":")     property(name="syslogseverity")
    constant(value="}\n")
}

# Forward all messages as JSON to the named pipe using the template
*.* action(type="ompipe" Pipe="/var/log/api_forward/api_pipe" template="ApiForwardJson")
EOF

echo "=== Stopping rsyslog to clear state ==="
sudo systemctl stop rsyslog
sleep 2

echo "=== Validating rsyslog config ==="
sudo rsyslogd -N1 -f /etc/rsyslog.conf
echo "Config validation complete"

echo "=== Starting rsyslog with new config ==="
sudo systemctl start rsyslog
sleep 2

echo "=== Checking what rsyslog actually wrote to pipe ==="
# Temporarily read from pipe to see actual output
timeout 2 cat /var/log/api_forward/api_pipe || true

echo "=== Creating systemd service ==="
sudo tee "$SERVICE" > /dev/null <<'EOF'
[Unit]
Description=Forward all logs from named pipe to API (resilient)
After=network.target rsyslog.service
Wants=rsyslog.service

[Service]
ExecStart=/usr/local/bin/send_log_api.sh
Restart=always
RestartSec=5s
User=syslog
Group=adm
SyslogIdentifier=api-forward

[Install]
WantedBy=multi-user.target
EOF

echo "=== Reloading systemd and starting services ==="
sudo systemctl daemon-reload
sudo systemctl enable --now send_log_api.service

echo "=== Setup complete! Check service logs with: ==="
echo "sudo journalctl -u send_log_api.service -f"

# ===== Optional: Configure HMAC Auth via systemd drop-in =====
echo "=== Configuring HMAC auth (optional) ==="
sudo mkdir -p /etc/systemd/system/send_log_api.service.d
sudo tee /etc/systemd/system/send_log_api.service.d/override.conf > /dev/null <<'EOF'
[Service]
Environment=HMAC_SECRET=MyKey
Environment=HMAC_HEADER=X-Signature
Environment=HMAC_TS_HEADER=X-Timestamp
Environment=HMAC_ALGO=sha256
EOF

sudo systemctl daemon-reload
sudo systemctl restart send_log_api.service
