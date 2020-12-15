package com.quorum.tessera.transaction;

import com.quorum.tessera.data.MessageHash;
import com.quorum.tessera.enclave.PrivacyMode;
import com.quorum.tessera.encryption.PublicKey;

import java.util.*;

public interface SendRequest {

    PublicKey getSender();

    List<PublicKey> getRecipients();

    byte[] getPayload();

    PrivacyMode getPrivacyMode();

    byte[] getExecHash();

    Set<MessageHash> getAffectedContractTransactions();

    class Builder {

        private PublicKey from;

        private List<PublicKey> recipients;

        private byte[] payload;

        private PrivacyMode privacyMode;

        private byte[] execHash;

        private Set<MessageHash> affectedContractTransactions;

        public static Builder create() {
            return new Builder() {};
        }

        public Builder withSender(PublicKey from) {
            this.from = from;
            return this;
        }

        public Builder withRecipients(List<PublicKey> recipients) {
            this.recipients = recipients;
            return this;
        }

        public Builder withAffectedContractTransactions(Set<MessageHash> affectedContractTransactions) {
            this.affectedContractTransactions = affectedContractTransactions;
            return this;
        }

        public Builder withPayload(byte[] payload) {
            this.payload = payload;
            return this;
        }

        public Builder withExecHash(byte[] execHash) {
            this.execHash = execHash;
            return this;
        }

        public Builder withPrivacyMode(PrivacyMode privacyMode) {
            this.privacyMode = privacyMode;
            return this;
        }

        public SendRequest build() {

            Objects.requireNonNull(from, "Sender is required");
            Objects.requireNonNull(recipients, "Recipients are required");
            Objects.requireNonNull(payload, "Payload is required");
            Objects.requireNonNull(privacyMode, "PrivacyMode is required");
            Objects.requireNonNull(affectedContractTransactions, "AffectedContractTransactions is required");
            Objects.requireNonNull(execHash, "ExecutionHash is required");

            if (privacyMode == PrivacyMode.PRIVATE_STATE_VALIDATION) {
                if (execHash.length == 0) {
                    throw new RuntimeException("ExecutionHash is required for PRIVATE_STATE_VALIDATION privacy mode");
                }
            }

            return new SendRequest() {

                @Override
                public PublicKey getSender() {
                    return from;
                }

                @Override
                public List<PublicKey> getRecipients() {
                    return List.copyOf(recipients);
                }

                @Override
                public byte[] getPayload() {
                    return Arrays.copyOf(payload, payload.length);
                }

                @Override
                public PrivacyMode getPrivacyMode() {
                    return privacyMode;
                }

                @Override
                public byte[] getExecHash() {
                    return execHash;
                }

                @Override
                public Set<MessageHash> getAffectedContractTransactions() {
                    return Set.copyOf(affectedContractTransactions);
                }
            };
        }
    }
}
