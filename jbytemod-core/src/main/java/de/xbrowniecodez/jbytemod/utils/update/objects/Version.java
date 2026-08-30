package de.xbrowniecodez.jbytemod.utils.update.objects;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class Version {
    private final int majorVersion;
    private final int minorVersion;
    private final int patchVersion;

    public Version(int majorVersion, int minorVersion, int patchVersion) {
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.patchVersion = patchVersion;
    }

    public Version(String version) {
        String[] split = version.split("\\.");
        // Ensure there are at least 3 parts; if not, default to 0 for missing parts
        this.majorVersion = split.length > 0 ? Integer.parseInt(split[0]) : 0;
        this.minorVersion = split.length > 1 ? Integer.parseInt(split[1]) : 0;
        this.patchVersion = split.length > 2 ? Integer.parseInt(split[2]) : 0;
    }
    /**
     * Compares this version to another version.
     *
     * @param version The version to compare to.
     * @return true if this version is greater than the other version, false otherwise.
     */
    public boolean isNewer(Version version) {
        if (this.majorVersion > version.majorVersion) {
            return true;
        } else if (this.majorVersion < version.majorVersion) {
            return false;
        } else { // majorVersion is equal
            if (this.minorVersion > version.minorVersion) {
                return true;
            } else if (this.minorVersion < version.minorVersion) {
                return false;
            } else { // minorVersion is equal
                return this.patchVersion > version.patchVersion;
            }
        }
    }


    public String toString() {
        return String.format("%d.%d.%d", majorVersion, minorVersion, patchVersion);
    }
}
