CREATE TABLE users (
                       user_id SERIAL PRIMARY KEY,
                       email VARCHAR(255) UNIQUE NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       role VARCHAR(50) NOT NULL,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       deleted_at TIMESTAMP
);

CREATE TABLE campuses (
                          campus_id SERIAL PRIMARY KEY,
                          name VARCHAR(255) NOT NULL,
                          city VARCHAR(100),
                          latitude DECIMAL(9,6),
                          longitude DECIMAL(9,6),
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          deleted_at TIMESTAMP
);

CREATE TABLE trips (
                       trip_id SERIAL PRIMARY KEY,
                       user_id INTEGER NOT NULL REFERENCES users(user_id),
                       campus_id INTEGER REFERENCES campuses(campus_id),
                       start_time TIMESTAMP NOT NULL,
                       end_time TIMESTAMP,
                       total_distance_m DECIMAL(12,2) DEFAULT 0.0,
                       total_carbon_g DECIMAL(12,2) DEFAULT 0.0,
                       source VARCHAR(50),
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       deleted_at TIMESTAMP
);

CREATE TABLE trip_segments (
                               trip_segment_id SERIAL PRIMARY KEY,
                               trip_id INTEGER NOT NULL REFERENCES trips(trip_id) ON DELETE CASCADE,
                               transport_mode VARCHAR(50) NOT NULL,
                               distance_m DECIMAL(12,2) NOT NULL,
                               carbon_g DECIMAL(12,2) NOT NULL,
                               segment_order INTEGER NOT NULL,
                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);