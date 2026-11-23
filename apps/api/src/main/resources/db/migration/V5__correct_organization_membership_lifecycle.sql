-- Suspension preserves membership history. Only removal ends membership.

ALTER TABLE organization_membership
DROP CONSTRAINT ck_organization_membership_lifecycle;

ALTER TABLE organization_membership
    ADD CONSTRAINT ck_organization_membership_lifecycle
        CHECK (
            (status IN ('ACTIVE', 'SUSPENDED') AND ended_at IS NULL)
                OR
            (status = 'REMOVED' AND ended_at IS NOT NULL)
            );