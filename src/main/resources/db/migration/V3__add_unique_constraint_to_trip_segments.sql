ALTER TABLE trip_segments
ADD CONSTRAINT uk_trip_segment_order UNIQUE (trip_id, segment_order);
