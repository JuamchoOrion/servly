-- Tabla para registrar solicitudes de ayuda de clientes
CREATE TABLE help_alerts (
    id SERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    table_number INTEGER NOT NULL,
    message VARCHAR(255),
    status VARCHAR(50) DEFAULT 'PENDING', -- PENDING, ATTENDED, CLOSED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    attended_at TIMESTAMP,
    attended_by VARCHAR(255) -- usuario que atendió
);

CREATE INDEX idx_help_alerts_table ON help_alerts(table_number);
CREATE INDEX idx_help_alerts_status ON help_alerts(status);
