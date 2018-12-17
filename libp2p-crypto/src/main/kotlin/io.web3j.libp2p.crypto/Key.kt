package io.web3j.libp2p.crypto

import crypto.pb.Crypto

enum class KEY_TYPE {
    /**
     * RSA is an enum for the supported RSA key type
     */
    RSA,

    /**
     * Ed25519 is an enum for the supported Ed25519 key type
     */
    ED25519,

    /**
     * Secp256k1 is an enum for the supported Secp256k1 key type
     */
    SECP256K1,

    /**
     * ECDSA is an enum for the supported ECDSA key type
     */
    ECDSA
}

interface Key {

    /**
     * Bytes returns a serialized, storeable representation of this key.
     */
    @Deprecated("Use marshal/unmarshal functions instead")
    fun bytes(): ByteArray

    /**
     * Equals checks whether two PubKeys are the same.
     */
    fun equals(other: Key): Boolean

    fun raw(): ByteArray

    fun type(): crypto.pb.Crypto.KeyType
}

/**
 * PrivKey represents a private key that can be used to generate a public key,
 * sign data, and decrypt data that was encrypted with a public key.
 */
interface PrivKey : Key {

    /**
     * Cryptographically sign the given bytes.
     */
    fun sign(data: ByteArray)

    /**
     * Return a public key paired with this private key.
     */
    fun publicKey(): PubKey
}

/**
 * PubKey is a public key.
 */
interface PubKey : Key {

    /**
     * Verify that 'sig' is the signed hash of 'data'.
     */
    fun verify(data: ByteArray, signature: ByteArray)
}

/**
 * Creates a PubKey from a given byte array.
 */
interface PublicKeyUnmarshaller {
    fun unmarshall(data: ByteArray): PubKey
}

/**
 * Creates a private key from a given byte array.
 */
interface PrivateKeyMarshaller {
    fun unmarshall(data: ByteArray): PrivKey
}

/**
 * Generates shared key from a given private key.
 */
interface GenSharedKey : (ByteArray) -> ByteArray

class BadKeyTypeException : Exception("Invalid or unsupported key type")

/**
 * Generate a new key pair of the provided type.
 */
fun generateKeyPair(type: KEY_TYPE, bits: Int): Pair<PrivKey, PubKey> {

    return when (type) {
        KEY_TYPE.RSA -> generateRsaKeyPair()
        KEY_TYPE.ED25519 -> generateEd25519KeyPair()
        KEY_TYPE.SECP256K1 -> generateSecp256k1KeyPair()
        KEY_TYPE.ECDSA -> generateEcdsaKeyPair()
    }
}

fun generateRsaKeyPair(): Pair<PrivKey, PubKey> = TODO()
fun generateEd25519KeyPair(): Pair<PrivKey, PubKey> = TODO()
fun generateSecp256k1KeyPair(): Pair<PrivKey, PubKey> = TODO()
fun generateEcdsaKeyPair(): Pair<PrivKey, PubKey> = TODO()
