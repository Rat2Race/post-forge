package dev.iamrat.study.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/** 테스트가 시간을 앞으로 돌릴 수 있는 시계. */
class MutableClock extends Clock {

    private final ZoneId zone;
    private Instant now;

    MutableClock(Instant now, ZoneId zone) {
        this.now = now;
        this.zone = zone;
    }

    void set(Instant instant) {
        this.now = instant;
    }

    void advance(Duration duration) {
        this.now = now.plus(duration);
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new MutableClock(now, zone);
    }

    @Override
    public Instant instant() {
        return now;
    }
}
