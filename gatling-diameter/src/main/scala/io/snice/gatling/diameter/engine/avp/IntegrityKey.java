package io.snice.gatling.diameter.engine.avp;

import java.util.function.Function;

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

public interface IntegrityKey extends Avp<OctetString> {
    int CODE = 626;
    Class<OctetString> TYPE = OctetString.class;
    Function<OctetString, IntegrityKey> CREATOR = IntegrityKey::of;

    static IntegrityKey of(Buffer value) {
        OctetString v = OctetString.parse(value);
        return of(v);
    }

    static IntegrityKey of(String value) {
        return of(Buffers.wrap(value));
    }

    static IntegrityKey of(OctetString value) {
        PreConditions.assertNotNull(value);
        Builder<OctetString> builder = Avp.ofType(OctetString.class).withValue(value).withAvpCode(626L).isMandatory(AvpMandatory.MUST.isMandatory()).isProtected(
                AvpProtected.MAY.isProtected()).withVendor(Vendor.TGPP);
        return new DefaultIntegrityKey(builder.build());
    }

    default long getCode() {
        return 626L;
    }

    default boolean isIntegrityKey() {
        return true;
    }

    default IntegrityKey toIntegrityKey() {
        return this;
    }

    static IntegrityKey parse(FramedAvp raw) {
        if (626L != raw.getCode()) {
            throw new AvpParseException("AVP Code mismatch - unable to parse the AVP into a " + IntegrityKey.class.getName());
        } else {
            return new DefaultIntegrityKey(raw);
        }
    }

    public static class DefaultIntegrityKey extends DiameterOctetStringAvp implements IntegrityKey {
        private DefaultIntegrityKey(FramedAvp raw) {
            super(raw);
        }

        public IntegrityKey ensure() {
            return this;
        }

        public boolean equals(Object other) {
            if (this == other) {
                return true;
            } else if (other == null) {
                return false;
            } else {
                try {
                    IntegrityKey o = (IntegrityKey)((FramedAvp)other).ensure();
                    OctetString v = (OctetString)this.getValue();
                    return v.equals(o.getValue());
                } catch (ClassCastException var4) {
                    return false;
                }
            }
        }
    }
}
