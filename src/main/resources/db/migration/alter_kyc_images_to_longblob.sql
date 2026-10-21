-- KYC photos are stored as binary image data and can exceed VARCHAR/TEXT limits.
-- Run this migration once on existing MySQL databases before submitting photos.
ALTER TABLE users
    MODIFY COLUMN identity_document_image LONGBLOB NULL,
    MODIFY COLUMN identity_selfie_image LONGBLOB NULL;
