/*
 * Copyright 2019 BLK Technologies Limited (web3labs.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package io.web3j.libp2p.crypto.keys

import crypto.pb.Crypto
import io.web3j.libp2p.crypto.ECDSA_ALGORITHM
import io.web3j.libp2p.crypto.KEY_PKCS8
import io.web3j.libp2p.crypto.Libp2pCrypto
import io.web3j.libp2p.crypto.P256_CURVE
import io.web3j.libp2p.crypto.PrivKey
import io.web3j.libp2p.crypto.PubKey
import io.web3j.libp2p.crypto.SHA_256_WITH_ECDSA
import io.web3j.libp2p.shared.env.Libp2pException
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec
import org.bouncycastle.jce.spec.ECPublicKeySpec
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.SecureRandom
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.security.PrivateKey as JavaPrivateKey
import java.security.interfaces.ECPrivateKey as JavaECPrivateKey

private val CURVE: ECNamedCurveParameterSpec = ECNamedCurveTable.getParameterSpec(P256_CURVE)

/**
 * @param priv the private key backing this instance.
 */
class EcdsaPrivateKey(private val priv: JavaPrivateKey) : PrivKey(Crypto.KeyType.ECDSA) {

    init {
        // Set up private key.
        if (priv.format != KEY_PKCS8) {
            throw Libp2pException("Private key must be of '$KEY_PKCS8' format")
        }
    }

    override fun raw(): ByteArray = priv.encoded

    /**
     * Sign the given bytes and returns the signature of the input data.
     * @param data the bytes to be signed.
     * @return the signature as a byte array.
     */
    override fun sign(data: ByteArray): ByteArray =
        with(Signature.getInstance(SHA_256_WITH_ECDSA, Libp2pCrypto.provider)) {
            // Signature is made up of r and s numbers.
            initSign(priv)
            update(data)
            sign()
        }

    override fun publicKey(): PubKey {
        val pubSpec: ECPublicKeySpec = (priv as BCECPrivateKey).run {
            val q = parameters.g.multiply((this as org.bouncycastle.jce.interfaces.ECPrivateKey).d)
            ECPublicKeySpec(q, parameters)
        }

        return with(KeyFactory.getInstance(ECDSA_ALGORITHM, Libp2pCrypto.provider)) {
            EcdsaPublicKey(generatePublic(pubSpec))
        }
    }

    override fun hashCode(): Int = priv.hashCode()
}

/**
 * @param pub the public key backing this instance.
 */
class EcdsaPublicKey(private val pub: PublicKey) : PubKey(Crypto.KeyType.ECDSA) {

    override fun raw(): ByteArray = pub.encoded

    override fun verify(data: ByteArray, signature: ByteArray): Boolean =
        with(Signature.getInstance(SHA_256_WITH_ECDSA, Libp2pCrypto.provider)) {
            initVerify(pub)
            update(data)
            verify(signature)
        }

    override fun hashCode(): Int = pub.hashCode()
}

/**
 * Generates a new ECDSA private and public key with a specified curve.
 * @param curve the curve spec.
 * @return a pair of private and public keys.
 */
private fun generateECDSAKeyPairWithCurve(curve: ECNamedCurveParameterSpec): Pair<PrivKey, PubKey> {
    val keypair: KeyPair = with(KeyPairGenerator.getInstance(ECDSA_ALGORITHM, Libp2pCrypto.provider)) {
        initialize(curve, SecureRandom())
        genKeyPair()
    }

    return Pair(EcdsaPrivateKey(keypair.private as JavaECPrivateKey), EcdsaPublicKey(keypair.public))
}

/**
 * Generates a new ECDSA private and public key pair.
 * @return a pair of private and public keys.
 */
fun generateEcdsaKeyPair(): Pair<PrivKey, PubKey> {
    // http://www.bouncycastle.org/wiki/display/JA1/Supported+Curves+%28ECDSA+and+ECGOST%29
    // and
    // http://www.bouncycastle.org/wiki/pages/viewpage.action?pageId=362269
    return generateECDSAKeyPairWithCurve(CURVE)
}

/**
 * Generates a new ecdsa private and public key from an input private key.
 * @param priv the private key.
 * @return a pair of private and public keys.
 */
fun ecdsaKeyPairFromKey(priv: EcdsaPrivateKey): Pair<PrivKey, PubKey> = Pair(priv, priv.publicKey())

/**
 * Unmarshals the given key bytes into an ECDSA private key instance.
 * @param keyBytes the key bytes.
 * @return a private key.
 */
fun unmarshalEcdsaPrivateKey(keyBytes: ByteArray): PrivKey = EcdsaPrivateKey(
    KeyFactory.getInstance(ECDSA_ALGORITHM, Libp2pCrypto.provider).generatePrivate(
        PKCS8EncodedKeySpec(keyBytes)
    )
)

/**
 * Unmarshals the given key bytes into an ECDSA public key instance.
 * @param keyBytes the key bytes.
 * @return a public key.
 */
fun unmarshalEcdsaPublicKey(keyBytes: ByteArray): PubKey =
    with(KeyFactory.getInstance(ECDSA_ALGORITHM, Libp2pCrypto.provider)) {
        EcdsaPublicKey(generatePublic(X509EncodedKeySpec(keyBytes)))
    }
