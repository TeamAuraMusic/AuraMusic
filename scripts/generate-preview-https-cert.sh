#!/usr/bin/env bash
set -euo pipefail

OUT="${1:?usage: generate-preview-https-cert.sh CERT_DIR LAN_IPV4}"
LAN_IP="${2:?usage: generate-preview-https-cert.sh CERT_DIR LAN_IPV4}"
command -v openssl >/dev/null || { echo 'openssl is required' >&2; exit 2; }
mkdir -p "$OUT"
openssl req -x509 -newkey rsa:3072 -sha256 -nodes -days 30 \
  -keyout "$OUT/preview-ca.key" -out "$OUT/preview-ca.crt" \
  -subj '/CN=AuraMusic PreviewProbe Local Test CA' \
  -addext 'basicConstraints=critical,CA:TRUE' \
  -addext 'keyUsage=critical,keyCertSign,cRLSign'
openssl req -new -newkey rsa:2048 -sha256 -nodes \
  -keyout "$OUT/preview-server.key" -out "$OUT/preview-server.csr" \
  -subj '/CN=AuraMusic PreviewProbe HTTPS'
cat > "$OUT/preview-server.ext" <<EOF
basicConstraints=critical,CA:FALSE
keyUsage=critical,digitalSignature,keyEncipherment
extendedKeyUsage=serverAuth
subjectAltName=IP:10.0.2.2,IP:${LAN_IP}
EOF
openssl x509 -req -sha256 -days 30 \
  -in "$OUT/preview-server.csr" \
  -CA "$OUT/preview-ca.crt" -CAkey "$OUT/preview-ca.key" -CAcreateserial \
  -extfile "$OUT/preview-server.ext" \
  -out "$OUT/preview-server.crt"
chmod 600 "$OUT/preview-ca.key" "$OUT/preview-server.key"
echo "Created diagnostic CA and server certificate in $OUT for 10.0.2.2 and $LAN_IP"
