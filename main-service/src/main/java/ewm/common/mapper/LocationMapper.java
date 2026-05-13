package ewm.common.mapper;

import ewm.common.dto.LocationDto;
import ewm.common.exception.BadRequestException;
import ewm.common.model.Location;

public class LocationMapper {
    public static Location mapLocation(LocationDto dto) {
        if (dto == null || dto.getLat() == null || dto.getLon() == null) {
            throw new BadRequestException("Location is required");
        }
        Location l = new Location();
        l.setLat(dto.getLat());
        l.setLon(dto.getLon());
        return l;
    }

    public static LocationDto mapLocationDto(Location loc) {
        if (loc == null) return null;
        LocationDto dto = new LocationDto();
        dto.setLat(loc.getLat());
        dto.setLon(loc.getLon());
        return dto;
    }

}
