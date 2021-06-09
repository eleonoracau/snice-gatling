package io.snice.gatling.diameter.engine.avp;

import io.snice.buffer.Buffer;
import io.snice.buffer.Buffers;
import io.snice.codecs.codec.diameter.avp.Avp;
import io.snice.codecs.codec.diameter.avp.AvpMandatory;
import io.snice.codecs.codec.diameter.avp.AvpParseException;
import io.snice.codecs.codec.diameter.avp.AvpProtected;
import io.snice.codecs.codec.diameter.avp.FramedAvp;
import io.snice.codecs.codec.diameter.avp.Vendor;
import io.snice.codecs.codec.diameter.avp.impl.DiameterOctetStringAvp;
import io.snice.codecs.codec.diameter.avp.type.OctetString;
import io.snice.preconditions.PreConditions;
import java.util.function.Function;

public interface ConfidentialityKey extends Avp<OctetString> {
    int CODE = 625;
    Class<OctetString> TYPE = OctetString.class;
    Function<OctetString, ConfidentialityKey> CREATOR = ConfidentialityKey::of;

    static ConfidentialityKey of(Buffer value) {
        OctetString v = OctetString.parse(value);
        return of(v);
    }

    static ConfidentialityKey of(String value) {
        return of(Buffers.wrap(value));
    }

    static ConfidentialityKey of(OctetString value) {
        PreConditions.assertNotNull(value);
        Builder<OctetString> builder = Avp.ofType(OctetString.class).withValue(value).withAvpCode(625L).isMandatory(AvpMandatory.MUST.isMandatory()).isProtected(AvpProtected.MAY.isProtected()).withVendor(Vendor.TGPP);
        return new DefaultConfidentialityKey(builder.build());
    }

    default long getCode() {
        return 625L;
    }

    default boolean isConfidentialityKey() {
        return true;
    }

    default ConfidentialityKey toConfidentialityKey() {
        return this;
    }

    static ConfidentialityKey parse(FramedAvp raw) {
        if (625L != raw.getCode()) {
            throw new AvpParseException("AVP Code mismatch - unable to parse the AVP into a " + ConfidentialityKey.class.getName());
        } else {
            return new DefaultConfidentialityKey(raw);
        }
    }

    public static class DefaultConfidentialityKey extends DiameterOctetStringAvp implements ConfidentialityKey {
        private DefaultConfidentialityKey(FramedAvp raw) {
            super(raw);
        }

        public ConfidentialityKey ensure() {
            return this;
        }

        public boolean equals(Object other) {
            if (this == other) {
                return true;
            } else if (other == null) {
                return false;
            } else {
                try {
                    ConfidentialityKey o = (ConfidentialityKey)((FramedAvp)other).ensure();
                    OctetString v = (OctetString)this.getValue();
                    return v.equals(o.getValue());
                } catch (ClassCastException var4) {
                    return false;
                }
            }
        }
    }
}
