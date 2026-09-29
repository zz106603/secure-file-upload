ALTER TABLE file_extension_policy
    ADD CONSTRAINT ck_file_extension_policy_custom_blocked
    CHECK (policy_type <> 'CUSTOM' OR blocked = TRUE);
