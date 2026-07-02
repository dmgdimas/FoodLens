FROM python:3.11-slim

WORKDIR /app

# Оптимизируем установку системных библиотек с использованием кэша apt
RUN --mount=type=cache,target=/var/cache/apt,sharing=locked \
    --mount=type=cache,target=/var/lib/apt,sharing=locked \
    apt-get update && apt-get install -y --no-install-recommends \
    libgl1 \
    libglib2.0-0 \
    libsm6 \
    libxext6 \
    libxrender1 \
    libxcb1

COPY requirements.txt .

# Используем кэш pip. Если requirements.txt изменится частично, 
# pip подтянет уже скачанные пакеты из кэша, а не будет качать их заново.
RUN --mount=type=cache,target=/root/.cache/pip \
    pip install --no-cache-dir -r requirements.txt

COPY . .

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "9000"]