package es.esi.techlab.bluebeetooth.model;

import java.util.UUID;

/**
 * <code>ProfileBase</code> interface is which we extends in order to implement the UUID from the
 * services of our bluetooth device with we plan to communicate with. Its mandatory that our device
 * has the UUID's included in this interface as minimum.
 * @see UUID
 */
public interface ProfileBase {

       UUID UUID_DESCRIPTOR_UPDATE_NOTIFICATION = UUID.fromString("");
       UUID UUID_CHAR_PAIR = UUID.fromString("");
       UUID UUID_CHAR_ACTIVITY_DATA = UUID.fromString("");
       UUID UUID_CHAR_FETCH = UUID.fromString("");

}
