package fr.xpdustry.domination;

import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import arc.util.serialization.Json.*;


class DominationSettings{
    public int zoneRadius = 5;

    public float
        captureRate = 10F,
        updateTicks = Time.toSeconds,
        renderTicks = Time.toSeconds / 6,
        gameDuration = Time.toMinutes * 5;

    public final ObjectMap<String, Seq<Zone>> maps = new ObjectMap<>();

    public static class DominationIO implements Serializer<DominationSettings>{
        @Override
        public void write(Json json, DominationSettings settings, Class aClass){
            json.writeObjectStart();

            json.writeValue("zone-radius",      settings.zoneRadius);
            json.writeValue("capture-rate",     settings.captureRate);
            json.writeValue("update-ticks",     settings.updateTicks);
            json.writeValue("render-ticks",     settings.renderTicks);
            json.writeValue("game-duration",    settings.gameDuration);

            json.writeArrayStart("maps"); // map array begin
            settings.maps.each((map, zones) -> {
                json.writeObjectStart();
                json.writeValue("name", map);

                json.writeArrayStart("zones"); // zone array begin
                zones.each(z -> json.writeValue(z.x + "," + z.y)); // zones pos are packed
                json.writeArrayEnd(); // zone array end

                json.writeObjectEnd();
            });
            json.writeArrayEnd(); // map array end

            json.writeObjectEnd();
        }

        @Override
        public DominationSettings read(Json json, JsonValue jsonValue, Class aClass){
            var settings = new DominationSettings();

            if(!jsonValue.isObject()){
                return settings;
            }

            settings.zoneRadius     = jsonValue.getInt(     "zone-radius",      settings.zoneRadius);
            settings.captureRate    = jsonValue.getFloat(   "capture-rate",     settings.captureRate);
            settings.updateTicks    = jsonValue.getFloat(   "update-ticks",     settings.updateTicks);
            settings.renderTicks    = jsonValue.getFloat(   "render-ticks",     settings.renderTicks);
            settings.gameDuration   = jsonValue.getFloat(   "game-duration",    settings.gameDuration);

            JsonValue maps = jsonValue.get("maps");
            if(!maps.isArray()){
                return settings;
            }

            for(var map : maps){
                if(!map.isObject()){
                    continue;
                }

                String name = map.getString("name");
                if(name == null) continue;
                settings.maps.put(name, new Seq<>(5));

                var zones = map.get("zones");
                if(!zones.isArray()) continue;
                for(var zone : zones.asStringArray()){
                    String[] pos = zone.split(",");
                    if(pos.length != 2) continue;

                    try{
                        settings.maps.get(name).add(new Zone(
                            Integer.parseInt(pos[0].trim()),
                            Integer.parseInt(pos[1].trim()),
                            settings.zoneRadius
                        ));
                    }catch(NumberFormatException ignored){
                    }
                }
            }

            return settings;
        }
    }
}
