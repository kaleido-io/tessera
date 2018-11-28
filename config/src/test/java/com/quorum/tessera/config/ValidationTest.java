package com.quorum.tessera.config;

import com.quorum.tessera.config.keypairs.*;
import org.junit.Test;
import org.mockito.Mockito;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    public void validateArgonOptions() {
        ArgonOptions options = new ArgonOptions("d", 10, 20, 30);

        Set<ConstraintViolation<ArgonOptions>> violations = validator.validate(options);

        assertThat(violations).isEmpty();

    }

    @Test
    public void validateArgonOptionsInvalidAlgo() {
        ArgonOptions options = new ArgonOptions("a", 10, 20, 30);

        Set<ConstraintViolation<ArgonOptions>> violations = validator.validate(options);

        assertThat(violations).hasSize(1);

    }

    @Test
    public void validateArgonOptionsAllNullAlgoHasDefaultValue() {
        ArgonOptions options = new ArgonOptions(null, null, null, null);

        Set<ConstraintViolation<ArgonOptions>> violations = validator.validate(options);

        assertThat(violations).hasSize(3);
        assertThat(options.getAlgorithm()).isEqualTo("id");

    }

    @Test
    public void keyDataConfigMissingPassword() {
        PrivateKeyData privateKeyData = new PrivateKeyData(null, "snonce", "asalt", "sbox", mock(ArgonOptions.class), null);
        KeyDataConfig keyDataConfig = new KeyDataConfig(privateKeyData, PrivateKeyType.LOCKED);
        KeyData keyData = new KeyData(keyDataConfig, "privateKey", "publicKey", null, null, null, null, null, null, null);
        Set<ConstraintViolation<KeyData>> violations = validator.validate(keyData);
        assertThat(violations).hasSize(1);

        ConstraintViolation<KeyData> violation = violations.iterator().next();

        assertThat(violation.getMessageTemplate()).isEqualTo("{ValidKeyDataConfig.message}");
        assertThat(violation.getPropertyPath().toString()).isEqualTo("config");
    }

    @Test
    public void keyDataConfigNaclFailure() {
        PrivateKeyData privateKeyData = new PrivateKeyData(null, "snonce", "asalt", "sbox", mock(ArgonOptions.class), "SECRET");
        KeyDataConfig keyDataConfig = new KeyDataConfig(privateKeyData, PrivateKeyType.LOCKED);
        KeyData keyData = new KeyData(keyDataConfig, "NACL_FAILURE", "publicKey", null, null, null, null, null, null, null);
        Set<ConstraintViolation<KeyData>> violations = validator.validate(keyData);
        assertThat(violations).hasSize(1);

        ConstraintViolation<KeyData> violation = violations.iterator().next();

        assertThat(violation.getMessageTemplate()).isEqualTo("Could not decrypt the private key with the provided password, please double check the passwords provided");
        assertThat(violation.getPropertyPath().toString()).isEqualTo("privateKey");
    }

    @Test
    public void keyDataConfigInvalidBase64() {
        PrivateKeyData privateKeyData = new PrivateKeyData(null, "snonce", "asalt", "sbox", mock(ArgonOptions.class), "SECRET");
        KeyDataConfig keyDataConfig = new KeyDataConfig(privateKeyData, PrivateKeyType.LOCKED);
        KeyData keyData = new KeyData(keyDataConfig, "INAVLID_BASE", "publicKey", null, null, null, null, null, null, null);
        Set<ConstraintViolation<KeyData>> violations = validator.validate(keyData);
        assertThat(violations).hasSize(1);

        ConstraintViolation<KeyData> violation = violations.iterator().next();

        assertThat(violation.getMessageTemplate()).isEqualTo("{ValidBase64.message}");
        assertThat(violation.getPropertyPath().toString()).isEqualTo("privateKey");
    }

    @Test
    public void inlineKeyPairNoPasswordProvided() {
        KeyDataConfig keyConfig = mock(KeyDataConfig.class);
        when(keyConfig.getType()).thenReturn(PrivateKeyType.LOCKED);
        when(keyConfig.getValue()).thenReturn("");

        InlineKeypair spy = Mockito.spy(new InlineKeypair("validkey", keyConfig));
        doReturn("validkey").when(spy).getPrivateKey();

        KeyConfiguration keyConfiguration = new KeyConfiguration(null, null, singletonList(spy), null, null);

        Set<ConstraintViolation<KeyConfiguration>> violations = validator.validate(keyConfiguration);

        assertThat(violations).hasSize(1);

        ConstraintViolation<KeyConfiguration> violation = violations.iterator().next();

        assertThat(violation.getMessage()).isEqualTo("A locked key was provided without a password.\n Please ensure the same number of passwords are provided as there are keys and remember to include empty passwords for unlocked keys");
    }

    @Test
    public void inlineKeyPairNaClFailure() {
        KeyDataConfig keyConfig = mock(KeyDataConfig.class);
        when(keyConfig.getType()).thenReturn(PrivateKeyType.UNLOCKED);
        when(keyConfig.getValue()).thenReturn("NACL_FAILURE");

        InlineKeypair keyPair = new InlineKeypair("validkey", keyConfig);

        Set<ConstraintViolation<InlineKeypair>> violations = validator.validate(keyPair);

        assertThat(violations).hasSize(1);

        ConstraintViolation<InlineKeypair> violation = violations.iterator().next();

        assertThat(violation.getMessageTemplate()).isEqualTo("Could not decrypt the private key with the provided password, please double check the passwords provided");
    }

    @Test
    public void directKeyPairInvalidBase64() {
        DirectKeyPair keyPair = new DirectKeyPair("INVALID_BASE", "INVALID_BASE");

        Set<ConstraintViolation<DirectKeyPair>> violations = validator.validate(keyPair);

        assertThat(violations).hasSize(2);

        Iterator<ConstraintViolation<DirectKeyPair>> iterator = violations.iterator();
        ConstraintViolation<DirectKeyPair> violation = iterator.next();

        assertThat(violation.getMessageTemplate()).isEqualTo("Invalid Base64 key provided");

        ConstraintViolation<DirectKeyPair> violation2 = iterator.next();

        assertThat(violation2.getMessageTemplate()).isEqualTo("Invalid Base64 key provided");
    }

    @Test
    public void inlineKeyPairInvalidBase64() {
        KeyDataConfig keyConfig = mock(KeyDataConfig.class);
        when(keyConfig.getType()).thenReturn(PrivateKeyType.UNLOCKED);
        when(keyConfig.getValue()).thenReturn("validkey");
        InlineKeypair keyPair = new InlineKeypair("INVALID_BASE", keyConfig);

        Set<ConstraintViolation<InlineKeypair>> violations = validator.validate(keyPair);

        assertThat(violations).hasSize(1);

        ConstraintViolation<InlineKeypair> violation = violations.iterator().next();

        assertThat(violation.getMessageTemplate()).isEqualTo("Invalid Base64 key provided");
        assertThat(violation.getPropertyPath().toString()).isEqualTo("publicKey");
    }

    @Test
    public void invalidAlwaysSendTo() {

        List<String> alwaysSendTo = singletonList("BOGUS");

        Config config = new Config(null, null, null, null, alwaysSendTo, null, false,false);

        Set<ConstraintViolation<Config>> violations = validator.validateProperty(config, "alwaysSendTo");

        assertThat(violations).hasSize(1);

        ConstraintViolation<Config> violation = violations.iterator().next();
        assertThat(violation.getPropertyPath().toString()).startsWith("alwaysSendTo[0]");
        assertThat(violation.getMessageTemplate()).isEqualTo("{ValidBase64.message}");
    }

    @Test
    public void validAlwaysSendTo() {

        String value = Base64.getEncoder().encodeToString("HELLOW".getBytes());

        List<String> alwaysSendTo = singletonList(value);

        Config config = new Config(null, null, null, null, alwaysSendTo, null, false,false);

        Set<ConstraintViolation<Config>> violations = validator.validateProperty(config, "alwaysSendTo");

        assertThat(violations).isEmpty();
    }

    @Test
    public void keypairPathsValidation() {

        final Path publicKeyPath = Paths.get(UUID.randomUUID().toString());
        final Path privateKeyPath = Paths.get(UUID.randomUUID().toString());

        final ConfigKeyPair keyPair = new FilesystemKeyPair(publicKeyPath, privateKeyPath);

        final KeyConfiguration keyConfiguration = new KeyConfiguration(null, null, singletonList(keyPair), null, null);

        final Set<ConstraintViolation<KeyConfiguration>> violations = validator.validate(keyConfiguration);
        assertThat(violations).hasSize(2);

        final Iterator<ConstraintViolation<KeyConfiguration>> iterator = violations.iterator();

        ConstraintViolation<KeyConfiguration> violation1 = iterator.next();
        assertThat(violation1.getMessageTemplate()).isEqualTo("File does not exist");

        ConstraintViolation<KeyConfiguration> violation2 = iterator.next();
        assertThat(violation2.getMessageTemplate()).isEqualTo("File does not exist");

        final List<String> paths = Arrays.asList(
            violation1.getPropertyPath().toString(), violation2.getPropertyPath().toString()
        );
        assertThat(paths).containsExactlyInAnyOrder("keyData[0].publicKeyPath", "keyData[0].privateKeyPath");
    }

    @Test
    public void keypairInlineValidation() {

        final ConfigKeyPair keyPair = new DirectKeyPair("notvalidbase64", "c==");

        KeyConfiguration keyConfiguration = new KeyConfiguration(null, null, singletonList(keyPair), null, null);

        Set<ConstraintViolation<KeyConfiguration>> violations = validator.validate(keyConfiguration);
        assertThat(violations).hasSize(1);

        ConstraintViolation<KeyConfiguration> violation = violations.iterator().next();

        assertThat(violation.getMessageTemplate()).isEqualTo("Invalid Base64 key provided");
        assertThat(violation.getPropertyPath().toString()).endsWith("privateKey");
    }

    @Test
    public void azureKeyPairIdsAllowedCharacterSetIsAlphanumericAndDash() {
        String keyVaultId = "0123456789-abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        AzureVaultKeyPair keyPair = new AzureVaultKeyPair(keyVaultId, keyVaultId);

        Set<ConstraintViolation<AzureVaultKeyPair>> violations = validator.validate(keyPair);
        assertThat(violations).hasSize(0);
    }

    @Test
    public void azureKeyPairIdsDisallowedCharactersCreateViolation() {
        String keyVaultId = "invalid_@!£$%^~^&_id";
        AzureVaultKeyPair keyPair = new AzureVaultKeyPair(keyVaultId, keyVaultId);

        Set<ConstraintViolation<AzureVaultKeyPair>> violations = validator.validate(keyPair);
        assertThat(violations).hasSize(2);

        assertThat(violations).extracting("messageTemplate")
                                .containsExactly("Azure Key Vault key IDs can only contain alphanumeric characters and dashes (-)",
                                    "Azure Key Vault key IDs can only contain alphanumeric characters and dashes (-)");
    }

    @Test
    public void azureKeyPairProvidedWithoutKeyVaultConfigCreatesViolation() {
        AzureVaultKeyPair keyPair = new AzureVaultKeyPair("publicVauldId", "privateVaultId");
        KeyConfiguration keyConfiguration = new KeyConfiguration(null, null, singletonList(keyPair), null, null);
        Config config = new Config(null, null, null, keyConfiguration, null, null, false, false);

        Set<ConstraintViolation<Config>> violations = validator.validateProperty(config, "keys");
        assertThat(violations).hasSize(1);

        ConstraintViolation<Config> violation = violations.iterator().next();
        assertThat(violation.getMessageTemplate()).isEqualTo("{ValidKeyVaultConfiguration.azure.message}");
    }

    @Test
    public void azureKeyPairProvidedWithHashicorpKeyVaultConfigCreatesViolation() {
        AzureVaultKeyPair keyPair = new AzureVaultKeyPair("publicVauldId", "privateVaultId");

        KeyConfiguration keyConfiguration = new KeyConfiguration();
        keyConfiguration.setKeyData(singletonList(keyPair));

        HashicorpKeyVaultConfig hashicorpConfig = new HashicorpKeyVaultConfig();
        keyConfiguration.setHashicorpKeyVaultConfig(hashicorpConfig);

        Config config = new Config();
        config.setKeys(keyConfiguration);

        Set<ConstraintViolation<Config>> violations = validator.validateProperty(config, "keys");
        assertThat(violations).hasSize(1);

        ConstraintViolation<Config> violation = violations.iterator().next();
        assertThat(violation.getMessageTemplate()).isEqualTo("{ValidKeyVaultConfiguration.azure.message}");
    }

    @Test
    public void hashicorpKeyPairProvidedWithoutKeyVaultConfigCreatesViolation() {
        HashicorpVaultKeyPair keyPair = new HashicorpVaultKeyPair("pubId", "privdId", "secretPath");

        KeyConfiguration keyConfiguration = new KeyConfiguration();
        keyConfiguration.setKeyData(singletonList(keyPair));
        keyConfiguration.setHashicorpKeyVaultConfig(null);

        Config config = new Config();
        config.setKeys(keyConfiguration);

        Set<ConstraintViolation<Config>> violations = validator.validateProperty(config, "keys");
        assertThat(violations).hasSize(1);

        ConstraintViolation<Config> violation = violations.iterator().next();
        assertThat(violation.getMessageTemplate()).isEqualTo("{ValidKeyVaultConfiguration.hashicorp.message}");
    }

    @Test
    public void hashicorpKeyPairProvidedWithAzureKeyVaultConfigCreatesViolation() {
        HashicorpVaultKeyPair keyPair = new HashicorpVaultKeyPair("pubId", "privdId", "secretPath");

        KeyConfiguration keyConfiguration = new KeyConfiguration();
        keyConfiguration.setKeyData(singletonList(keyPair));
        keyConfiguration.setHashicorpKeyVaultConfig(null);

        AzureKeyVaultConfig azureConfig = new AzureKeyVaultConfig();
        keyConfiguration.setAzureKeyVaultConfig(azureConfig);

        Config config = new Config();
        config.setKeys(keyConfiguration);

        Set<ConstraintViolation<Config>> violations = validator.validateProperty(config, "keys");
        assertThat(violations).hasSize(1);

        ConstraintViolation<Config> violation = violations.iterator().next();
        assertThat(violation.getMessageTemplate()).isEqualTo("{ValidKeyVaultConfiguration.hashicorp.message}");
    }

    @Test
    public void nonKeyVaultPairProvidedWithoutAzureAndHashicorpKeyVaultConfigDoesNotCreateViolation() {
        DirectKeyPair keyPair = new DirectKeyPair("pub", "priv");

        KeyConfiguration keyConfiguration = new KeyConfiguration(null, null, singletonList(keyPair), null, null);
        Config config = new Config(null, null, null, keyConfiguration, null, null, false, false);

        Set<ConstraintViolation<Config>> violations = validator.validateProperty(config, "keys");
        assertThat(violations).hasSize(0);
    }

    @Test
    public void keyConfigurationIsNullCreatesNotNullViolation() {
        Config config = new Config(null, null, null, null, null, null, false, false);

        Set<ConstraintViolation<Config>> violations = validator.validateProperty(config, "keys");

        assertThat(violations).hasSize(1);

        ConstraintViolation<Config> violation = violations.iterator().next();
        assertThat(violation.getMessageTemplate()).isEqualTo("{javax.validation.constraints.NotNull.message}");
    }

    @Test
    public void azureVaultConfigWithNoUrlCreatesNullViolation() {
        AzureKeyVaultConfig keyVaultConfig = new AzureKeyVaultConfig(null);

        Set<ConstraintViolation<AzureKeyVaultConfig>> violations = validator.validate(keyVaultConfig);
        assertThat(violations).hasSize(1);

        ConstraintViolation<AzureKeyVaultConfig> violation = violations.iterator().next();
        assertThat(violation.getMessageTemplate()).isEqualTo("{javax.validation.constraints.NotNull.message}");
    }

    @Test
    public void azureVaultKeyPairProvidedButKeyVaultConfigHasNullUrlCreatesNotNullViolation() {
        AzureVaultKeyPair keyPair = new AzureVaultKeyPair("pubId", "privId");
        AzureKeyVaultConfig keyVaultConfig = new AzureKeyVaultConfig(null);
        KeyConfiguration keyConfiguration = new KeyConfiguration(null, null, singletonList(keyPair), keyVaultConfig, null);

        Set<ConstraintViolation<KeyConfiguration>> violations = validator.validate(keyConfiguration);
        assertThat(violations).hasSize(1);

        ConstraintViolation<KeyConfiguration> violation = violations.iterator().next();
        assertThat(violation.getMessageTemplate()).isEqualTo("{javax.validation.constraints.NotNull.message}");
        assertThat(violation.getPropertyPath().toString()).isEqualTo("azureKeyVaultConfig.url");
    }

    @Test
    public void hashicorpVaultConfigWithNoUrlCreatesNotNullViolation() {
        HashicorpKeyVaultConfig keyVaultConfig = new HashicorpKeyVaultConfig(null);

        Set<ConstraintViolation<HashicorpKeyVaultConfig>> violations = validator.validate(keyVaultConfig);
        assertThat(violations).hasSize(1);

        ConstraintViolation<HashicorpKeyVaultConfig> violation = violations.iterator().next();
        assertThat(violation.getMessageTemplate()).isEqualTo("{javax.validation.constraints.NotNull.message}");
    }

    @Test
    public void hashicorpVaultKeyPairProvidedButKeyVaultConfigHasNullUrlCreatesNotNullViolation() {
        HashicorpVaultKeyPair keyPair = new HashicorpVaultKeyPair("pubId", "privId", "secretPath");
        HashicorpKeyVaultConfig keyVaultConfig = new HashicorpKeyVaultConfig(null);
        KeyConfiguration keyConfiguration = new KeyConfiguration(null, null, singletonList(keyPair), null, keyVaultConfig);

        Set<ConstraintViolation<KeyConfiguration>> violations = validator.validate(keyConfiguration);
        assertThat(violations).hasSize(1);

        ConstraintViolation<KeyConfiguration> violation = violations.iterator().next();
        assertThat(violation.getMessageTemplate()).isEqualTo("{javax.validation.constraints.NotNull.message}");
        assertThat(violation.getPropertyPath().toString()).isEqualTo("hashicorpKeyVaultConfig.url");
    }
}
