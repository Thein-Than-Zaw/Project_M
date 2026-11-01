-- DEV-02 / D01 / AC-F01-08: stable lock serializes token issuance and signup.
CREATE TABLE registration_gate (
    id SMALLINT PRIMARY KEY CHECK (id = 1)
);
INSERT INTO registration_gate(id) VALUES (1);
