ALTER TABLE member
ADD CONSTRAINT uk_member_team_email
UNIQUE (team_id, email);