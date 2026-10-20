-- CreateTable
CREATE TABLE "admin_users" (
    "id" TEXT NOT NULL,
    "email" TEXT NOT NULL,
    "password_hash" TEXT NOT NULL,
    "role" TEXT NOT NULL DEFAULT 'RESEARCHER',
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "admin_users_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "participants" (
    "id" TEXT NOT NULL,
    "pseudonym_id" TEXT NOT NULL,
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "participants_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "sessions" (
    "id" TEXT NOT NULL,
    "session_id" TEXT NOT NULL,
    "test_id" TEXT NOT NULL,
    "participant_id" TEXT,
    "start_time" TIMESTAMP(3) NOT NULL,
    "end_time" TIMESTAMP(3),
    "orientation" TEXT NOT NULL DEFAULT 'portrait',
    "device_model" TEXT,
    "available_sensors" TEXT,
    "status" TEXT NOT NULL DEFAULT 'PENDING',
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "sessions_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "action_events" (
    "id" TEXT NOT NULL,
    "event_id" BIGINT NOT NULL,
    "session_id" TEXT NOT NULL,
    "timestamp" BIGINT NOT NULL,
    "elapsed_ms" BIGINT NOT NULL,
    "activity" TEXT NOT NULL,
    "orientation" TEXT NOT NULL,
    "expected_action" TEXT NOT NULL,
    "detected_action" TEXT NOT NULL,
    "result" TEXT NOT NULL,
    "attempt_id" INTEGER NOT NULL DEFAULT 1,
    "duration_ms" BIGINT NOT NULL DEFAULT 0,
    "touch_info" TEXT DEFAULT '',

    CONSTRAINT "action_events_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "sensor_samples" (
    "id" TEXT NOT NULL,
    "session_id" TEXT NOT NULL,
    "timestamp" BIGINT NOT NULL,
    "elapsed_ms" BIGINT NOT NULL,
    "activity" TEXT NOT NULL,
    "acc_x" DOUBLE PRECISION NOT NULL,
    "acc_y" DOUBLE PRECISION NOT NULL,
    "acc_z" DOUBLE PRECISION NOT NULL,
    "gyro_x" DOUBLE PRECISION NOT NULL,
    "gyro_y" DOUBLE PRECISION NOT NULL,
    "gyro_z" DOUBLE PRECISION NOT NULL,
    "proximity" DOUBLE PRECISION NOT NULL,
    "acc_magnitude" DOUBLE PRECISION NOT NULL,
    "gyro_magnitude" DOUBLE PRECISION NOT NULL,

    CONSTRAINT "sensor_samples_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "upload_batches" (
    "id" TEXT NOT NULL,
    "session_id" TEXT NOT NULL,
    "batch_index" INTEGER NOT NULL,
    "status" TEXT NOT NULL DEFAULT 'RECEIVED',
    "received_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "upload_batches_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "audit_logs" (
    "id" TEXT NOT NULL,
    "admin_id" TEXT,
    "action" TEXT NOT NULL,
    "details" TEXT,
    "ip_address" TEXT,
    "timestamp" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "audit_logs_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE UNIQUE INDEX "admin_users_email_key" ON "admin_users"("email");

-- CreateIndex
CREATE UNIQUE INDEX "participants_pseudonym_id_key" ON "participants"("pseudonym_id");

-- CreateIndex
CREATE UNIQUE INDEX "sessions_session_id_key" ON "sessions"("session_id");

-- CreateIndex
CREATE UNIQUE INDEX "action_events_event_id_key" ON "action_events"("event_id");

-- AddForeignKey
ALTER TABLE "sessions" ADD CONSTRAINT "sessions_participant_id_fkey" FOREIGN KEY ("participant_id") REFERENCES "participants"("id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "action_events" ADD CONSTRAINT "action_events_session_id_fkey" FOREIGN KEY ("session_id") REFERENCES "sessions"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "sensor_samples" ADD CONSTRAINT "sensor_samples_session_id_fkey" FOREIGN KEY ("session_id") REFERENCES "sessions"("id") ON DELETE CASCADE ON UPDATE CASCADE;

