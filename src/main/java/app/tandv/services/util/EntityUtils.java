package app.tandv.services.util;

/**
 * @author Vic on 8/31/2018
 **/
public final class EntityUtils {
    private EntityUtils() {
    }

    public static int entityHash(Object... objects) {
        int result = 1;
        for (Object object : objects) {
            result = 37 * result + objectHash(object);
        }
        return result;
    }

    private static int objectHash(Object object) {
        if (object == null)
            return 0;
        if (object instanceof Boolean)
            return (Boolean) object ? 0 : 1;
        if (object instanceof Byte)
            return ((Byte) object).intValue();
        if (object instanceof Character)
            return (Character) object;
        if (object instanceof Short)
            return ((Short) object).intValue();
        if (object instanceof Integer)
            return (int) object;
        if (object instanceof Long)
            return (int) ((long) object ^ ((long) object >>> 32));
        if (object instanceof Float)
            return Float.floatToIntBits((float) object);
        if (object instanceof Double) {
            long longHash = Double.doubleToLongBits((double) object);
            return (int) (longHash ^ (longHash >>> 32));
        }
        if (object instanceof String)
            return object.hashCode();
        if (object instanceof Enum)
            return ((Enum) object).name().hashCode();
        return 0;
    }
}
