package com.nova.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

public class SpaceBriefCache implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Instant fetchedAt;
    private IssPosition issPosition;
    private List<Launch> upcomingLaunches;
    private List<NearEarthObject> nearEarthObjects;
    private SpaceWeather spaceWeather;
    private Apod apod;

    public SpaceBriefCache() {}
    public SpaceBriefCache(Instant fetchedAt, IssPosition issPosition, List<Launch> upcomingLaunches,
                           List<NearEarthObject> nearEarthObjects, SpaceWeather spaceWeather, Apod apod) {
        this.fetchedAt = fetchedAt; this.issPosition = issPosition;
        this.upcomingLaunches = upcomingLaunches; this.nearEarthObjects = nearEarthObjects;
        this.spaceWeather = spaceWeather; this.apod = apod;
    }

    @JsonSerialize(using = ToStringSerializer.class)
    public Instant getFetchedAt() { return fetchedAt; }
    public void setFetchedAt(Instant v) { fetchedAt = v; }
    public IssPosition getIssPosition() { return issPosition; }
    public void setIssPosition(IssPosition v) { issPosition = v; }
    public List<Launch> getUpcomingLaunches() { return upcomingLaunches; }
    public void setUpcomingLaunches(List<Launch> v) { upcomingLaunches = v; }
    public List<NearEarthObject> getNearEarthObjects() { return nearEarthObjects; }
    public void setNearEarthObjects(List<NearEarthObject> v) { nearEarthObjects = v; }
    public SpaceWeather getSpaceWeather() { return spaceWeather; }
    public void setSpaceWeather(SpaceWeather v) { spaceWeather = v; }
    public Apod getApod() { return apod; }
    public void setApod(Apod v) { apod = v; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private Instant fetchedAt; private IssPosition issPosition;
        private List<Launch> upcomingLaunches; private List<NearEarthObject> nearEarthObjects;
        private SpaceWeather spaceWeather; private Apod apod;
        public Builder fetchedAt(Instant v) { fetchedAt = v; return this; }
        public Builder issPosition(IssPosition v) { issPosition = v; return this; }
        public Builder upcomingLaunches(List<Launch> v) { upcomingLaunches = v; return this; }
        public Builder nearEarthObjects(List<NearEarthObject> v) { nearEarthObjects = v; return this; }
        public Builder spaceWeather(SpaceWeather v) { spaceWeather = v; return this; }
        public Builder apod(Apod v) { apod = v; return this; }
        public SpaceBriefCache build() {
            return new SpaceBriefCache(fetchedAt, issPosition, upcomingLaunches, nearEarthObjects, spaceWeather, apod);
        }
    }

    // ─── IssPosition ─────────────────────────────────────────────────────────
    public static class IssPosition implements Serializable {
        private double latitude, longitude, altitudeKm, speedKms;
        private Instant timestamp;
        public IssPosition() {}
        public IssPosition(double latitude, double longitude, double altitudeKm, double speedKms, Instant timestamp) {
            this.latitude = latitude; this.longitude = longitude;
            this.altitudeKm = altitudeKm; this.speedKms = speedKms; this.timestamp = timestamp;
        }
        public double getLatitude() { return latitude; } public void setLatitude(double v) { latitude = v; }
        public double getLongitude() { return longitude; } public void setLongitude(double v) { longitude = v; }
        public double getAltitudeKm() { return altitudeKm; } public void setAltitudeKm(double v) { altitudeKm = v; }
        public double getSpeedKms() { return speedKms; } public void setSpeedKms(double v) { speedKms = v; }
        @JsonSerialize(using = ToStringSerializer.class)
        public Instant getTimestamp() { return timestamp; } public void setTimestamp(Instant v) { timestamp = v; }
        public static Builder builder() { return new Builder(); }
        public static class Builder {
            private double latitude, longitude, altitudeKm, speedKms; private Instant timestamp;
            public Builder latitude(double v) { latitude = v; return this; }
            public Builder longitude(double v) { longitude = v; return this; }
            public Builder altitudeKm(double v) { altitudeKm = v; return this; }
            public Builder speedKms(double v) { speedKms = v; return this; }
            public Builder timestamp(Instant v) { timestamp = v; return this; }
            public IssPosition build() { return new IssPosition(latitude, longitude, altitudeKm, speedKms, timestamp); }
        }
    }

    // ─── Launch ───────────────────────────────────────────────────────────────
    public static class Launch implements Serializable {
        private String id, name, agency, agencyCode, rocket, launchSite, launchSiteCountry, status, missionDesc, imageUrl;
        private Instant windowStart;
        public Launch() {}
        public String getId() { return id; } public void setId(String v) { id = v; }
        public String getName() { return name; } public void setName(String v) { name = v; }
        public String getAgency() { return agency; } public void setAgency(String v) { agency = v; }
        public String getAgencyCode() { return agencyCode; } public void setAgencyCode(String v) { agencyCode = v; }
        public String getRocket() { return rocket; } public void setRocket(String v) { rocket = v; }
        public String getLaunchSite() { return launchSite; } public void setLaunchSite(String v) { launchSite = v; }
        public String getLaunchSiteCountry() { return launchSiteCountry; } public void setLaunchSiteCountry(String v) { launchSiteCountry = v; }
        @JsonSerialize(using = ToStringSerializer.class)
        public Instant getWindowStart() { return windowStart; } public void setWindowStart(Instant v) { windowStart = v; }
        public String getStatus() { return status; } public void setStatus(String v) { status = v; }
        public String getMissionDesc() { return missionDesc; } public void setMissionDesc(String v) { missionDesc = v; }
        public String getImageUrl() { return imageUrl; } public void setImageUrl(String v) { imageUrl = v; }
        public static Builder builder() { return new Builder(); }
        public static class Builder {
            private String id, name, agency, agencyCode, rocket, launchSite, launchSiteCountry, status, missionDesc, imageUrl;
            private Instant windowStart;
            public Builder id(String v) { id = v; return this; }
            public Builder name(String v) { name = v; return this; }
            public Builder agency(String v) { agency = v; return this; }
            public Builder agencyCode(String v) { agencyCode = v; return this; }
            public Builder rocket(String v) { rocket = v; return this; }
            public Builder launchSite(String v) { launchSite = v; return this; }
            public Builder launchSiteCountry(String v) { launchSiteCountry = v; return this; }
            public Builder windowStart(Instant v) { windowStart = v; return this; }
            public Builder status(String v) { status = v; return this; }
            public Builder missionDesc(String v) { missionDesc = v; return this; }
            public Builder imageUrl(String v) { imageUrl = v; return this; }
            public Launch build() {
                Launch l = new Launch();
                l.id = id; l.name = name; l.agency = agency; l.agencyCode = agencyCode;
                l.rocket = rocket; l.launchSite = launchSite; l.launchSiteCountry = launchSiteCountry;
                l.windowStart = windowStart; l.status = status; l.missionDesc = missionDesc; l.imageUrl = imageUrl;
                return l;
            }
        }
    }

    // ─── NearEarthObject ─────────────────────────────────────────────────────
    public static class NearEarthObject implements Serializable {
        private String id, name;
        private double distanceAu, distanceKm, diameterMinKm, diameterMaxKm, relativeVelocityKms;
        private boolean potentiallyHazardous;
        private Instant closeApproachDate;
        public NearEarthObject() {}
        public String getId() { return id; } public void setId(String v) { id = v; }
        public String getName() { return name; } public void setName(String v) { name = v; }
        public double getDistanceAu() { return distanceAu; } public void setDistanceAu(double v) { distanceAu = v; }
        public double getDistanceKm() { return distanceKm; } public void setDistanceKm(double v) { distanceKm = v; }
        public double getDiameterMinKm() { return diameterMinKm; } public void setDiameterMinKm(double v) { diameterMinKm = v; }
        public double getDiameterMaxKm() { return diameterMaxKm; } public void setDiameterMaxKm(double v) { diameterMaxKm = v; }
        public double getRelativeVelocityKms() { return relativeVelocityKms; } public void setRelativeVelocityKms(double v) { relativeVelocityKms = v; }
        public boolean isPotentiallyHazardous() { return potentiallyHazardous; } public void setPotentiallyHazardous(boolean v) { potentiallyHazardous = v; }
        @JsonSerialize(using = ToStringSerializer.class)
        public Instant getCloseApproachDate() { return closeApproachDate; } public void setCloseApproachDate(Instant v) { closeApproachDate = v; }
        public static Builder builder() { return new Builder(); }
        public static class Builder {
            private String id, name;
            private double distanceAu, distanceKm, diameterMinKm, diameterMaxKm, relativeVelocityKms;
            private boolean potentiallyHazardous; private Instant closeApproachDate;
            public Builder id(String v) { id = v; return this; }
            public Builder name(String v) { name = v; return this; }
            public Builder distanceAu(double v) { distanceAu = v; return this; }
            public Builder distanceKm(double v) { distanceKm = v; return this; }
            public Builder diameterMinKm(double v) { diameterMinKm = v; return this; }
            public Builder diameterMaxKm(double v) { diameterMaxKm = v; return this; }
            public Builder relativeVelocityKms(double v) { relativeVelocityKms = v; return this; }
            public Builder potentiallyHazardous(boolean v) { potentiallyHazardous = v; return this; }
            public Builder closeApproachDate(Instant v) { closeApproachDate = v; return this; }
            public NearEarthObject build() {
                NearEarthObject n = new NearEarthObject();
                n.id = id; n.name = name; n.distanceAu = distanceAu; n.distanceKm = distanceKm;
                n.diameterMinKm = diameterMinKm; n.diameterMaxKm = diameterMaxKm;
                n.relativeVelocityKms = relativeVelocityKms; n.potentiallyHazardous = potentiallyHazardous;
                n.closeApproachDate = closeApproachDate; return n;
            }
        }
    }

    // ─── SpaceWeather ────────────────────────────────────────────────────────
    public static class SpaceWeather implements Serializable {
        private double kpIndex; private String stormLevel;
        private List<SolarFlare> flares; private Instant fetchedAt;
        public SpaceWeather() {}
        public double getKpIndex() { return kpIndex; } public void setKpIndex(double v) { kpIndex = v; }
        public String getStormLevel() { return stormLevel; } public void setStormLevel(String v) { stormLevel = v; }
        public List<SolarFlare> getFlares() { return flares; } public void setFlares(List<SolarFlare> v) { flares = v; }
        @JsonSerialize(using = ToStringSerializer.class)
        public Instant getFetchedAt() { return fetchedAt; } public void setFetchedAt(Instant v) { fetchedAt = v; }
        public static Builder builder() { return new Builder(); }
        public static class Builder {
            private double kpIndex; private String stormLevel;
            private List<SolarFlare> flares; private Instant fetchedAt;
            public Builder kpIndex(double v) { kpIndex = v; return this; }
            public Builder stormLevel(String v) { stormLevel = v; return this; }
            public Builder flares(List<SolarFlare> v) { flares = v; return this; }
            public Builder fetchedAt(Instant v) { fetchedAt = v; return this; }
            public SpaceWeather build() {
                SpaceWeather w = new SpaceWeather();
                w.kpIndex = kpIndex; w.stormLevel = stormLevel; w.flares = flares; w.fetchedAt = fetchedAt; return w;
            }
        }

        public static class SolarFlare implements Serializable {
            private String classType, status, region;
            private Instant beginTime, peakTime, endTime;
            public SolarFlare() {}
            public String getClassType() { return classType; } public void setClassType(String v) { classType = v; }
            public String getStatus() { return status; } public void setStatus(String v) { status = v; }
            public String getRegion() { return region; } public void setRegion(String v) { region = v; }
            @JsonSerialize(using = ToStringSerializer.class)
            public Instant getBeginTime() { return beginTime; } public void setBeginTime(Instant v) { beginTime = v; }
            @JsonSerialize(using = ToStringSerializer.class)
            public Instant getPeakTime() { return peakTime; } public void setPeakTime(Instant v) { peakTime = v; }
            @JsonSerialize(using = ToStringSerializer.class)
            public Instant getEndTime() { return endTime; } public void setEndTime(Instant v) { endTime = v; }
            public static Builder builder() { return new Builder(); }
            public static class Builder {
                private String classType, status, region;
                private Instant beginTime, peakTime, endTime;
                public Builder classType(String v) { classType = v; return this; }
                public Builder status(String v) { status = v; return this; }
                public Builder region(String v) { region = v; return this; }
                public Builder beginTime(Instant v) { beginTime = v; return this; }
                public Builder peakTime(Instant v) { peakTime = v; return this; }
                public Builder endTime(Instant v) { endTime = v; return this; }
                public SolarFlare build() {
                    SolarFlare f = new SolarFlare();
                    f.classType = classType; f.status = status; f.region = region;
                    f.beginTime = beginTime; f.peakTime = peakTime; f.endTime = endTime; return f;
                }
            }
        }
    }

    // ─── Apod ────────────────────────────────────────────────────────────────
    public static class Apod implements Serializable {
        private String title, explanation, url, hdUrl, mediaType, date, copyright;
        public Apod() {}
        public String getTitle() { return title; } public void setTitle(String v) { title = v; }
        public String getExplanation() { return explanation; } public void setExplanation(String v) { explanation = v; }
        public String getUrl() { return url; } public void setUrl(String v) { url = v; }
        public String getHdUrl() { return hdUrl; } public void setHdUrl(String v) { hdUrl = v; }
        public String getMediaType() { return mediaType; } public void setMediaType(String v) { mediaType = v; }
        public String getDate() { return date; } public void setDate(String v) { date = v; }
        public String getCopyright() { return copyright; } public void setCopyright(String v) { copyright = v; }
        public static Builder builder() { return new Builder(); }
        public static class Builder {
            private String title, explanation, url, hdUrl, mediaType, date, copyright;
            public Builder title(String v) { title = v; return this; }
            public Builder explanation(String v) { explanation = v; return this; }
            public Builder url(String v) { url = v; return this; }
            public Builder hdUrl(String v) { hdUrl = v; return this; }
            public Builder mediaType(String v) { mediaType = v; return this; }
            public Builder date(String v) { date = v; return this; }
            public Builder copyright(String v) { copyright = v; return this; }
            public Apod build() {
                Apod a = new Apod();
                a.title = title; a.explanation = explanation; a.url = url; a.hdUrl = hdUrl;
                a.mediaType = mediaType; a.date = date; a.copyright = copyright; return a;
            }
        }
    }
}
