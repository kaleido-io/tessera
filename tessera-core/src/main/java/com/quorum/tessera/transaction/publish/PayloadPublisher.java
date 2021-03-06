package com.quorum.tessera.transaction.publish;

import com.quorum.tessera.enclave.EncodedPayload;
import com.quorum.tessera.encryption.PublicKey;

/** Publishes messages from one node to another */
public interface PayloadPublisher {

  /**
   * Publishes an EncodedPayload to the recipient identified by the provided key
   *
   * @param payload the payload object to be pushed
   * @param recipientKey the public key identifying the target node
   */
  void publishPayload(EncodedPayload payload, PublicKey recipientKey);
}
