/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.protocol.blame;

import com.shuffle.bitcoin.DecryptionKey;
import com.shuffle.bitcoin.Transaction;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.p2p.Bytestring;
import com.shuffle.protocol.message.Packet;

import java.io.Serializable;
import java.util.Queue;

/**
 * Represents a message in which a player indicates that something has gone wrong, and which
 * possibly implicates another player as the culprit.
 *
 * Created by Daniel Krawisz on 1/22/16.
 */
public class Blame implements Serializable {
    public final Reason reason;
    public final VerificationKey accused; // Can be null if we don't know who to accuse yet.
    public final Transaction t;
    public final Queue<Packet> packets;
    public final DecryptionKey privateKey;
    public final Bytestring invalid; // Used for an invalid signature.

    public Blame(Reason reason,
                  VerificationKey accused,
                  Transaction t,
                  DecryptionKey privateKey,
                  Bytestring invalid,
                  Queue<Packet> packets) {

        if (reason == null) {
            throw new IllegalArgumentException();
        }

        switch (reason) {
            case InsufficientFunds: {
                if (accused == null) throw new IllegalArgumentException();
                break;
            }
            case DoubleSpend: {
                if (accused == null || t == null) {
                    throw new IllegalArgumentException();
                }
                break;
            }
            case MissingOutput:
            case ShuffleFailure: {
                if (accused == null) {
                    throw new IllegalArgumentException();
                }
                break;
            }
            case EquivocationFailure: {
                if (packets == null) {
                    throw new IllegalArgumentException();
                }
                break;
            }
            case ShuffleAndEquivocationFailure: {
                if (packets == null) {
                    throw new IllegalArgumentException();
                }
                break;
            }
            case InvalidSignature: {
                if (invalid == null || accused == null) {
                    throw new IllegalArgumentException();
                }
                break;
            }
            default:
                throw new IllegalArgumentException();
        }

        this.reason = reason;
        this.accused = accused;
        this.t = t;
        this.packets = packets;
        this.invalid = invalid;
        this.privateKey = privateKey;
    }

    @Override
    public String toString() {
        String str = "Blame[";
        if (accused != null) {
            str += ("accused: " + accused.toString() + ", ");
        }
        if (packets != null) {
            str += ("packets: " + packets.toString() + ", ");
        }
        return str + reason.toString() + "]";
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        
        if (!(o instanceof Blame)) {
            return false;
        }
        
        Blame blame = (Blame)o;
        
        return (reason == blame.reason)
                && (accused == blame.accused || accused != null && accused.equals(blame.accused))
                && (t == blame.t || t != null && t.equals(blame.t))
                && (packets == blame.packets || packets != null && packets.equals(blame.packets))
                && (privateKey == blame.privateKey
                    || privateKey != null && privateKey.equals(blame.privateKey))
                && (invalid == blame.invalid || invalid != null && invalid.equals(blame.invalid));
    }

    // Sent when a player has insufficient funds in his address.
    public static Blame InsufficientFunds(VerificationKey accused) {
        return new Blame(Reason.InsufficientFunds, accused, null, null, null, null);
    }

    // Sent when a player spends his coins while the protocol is in motion.
    public static Blame DoubleSpend(VerificationKey accused, Transaction t) {
        return new Blame(Reason.DoubleSpend, accused, t, null, null, null);
    }

    // Sent when a player makes an invalid signature to the transaction.
    public static Blame InvalidSignature(VerificationKey accused, Bytestring invalid) {
        return new Blame(Reason.InvalidSignature, accused, null, null, invalid, null);
    }

    // Sent when something goes wrong in phase 4.
    public static Blame EquivocationFailure(Queue<Packet> packets) {
        return new Blame(Reason.EquivocationFailure, null, null, null, null, packets);
    }

    // Sent when something goes wrong in phase 2.
    public static Blame ShuffleFailure(VerificationKey accused) {
        return new Blame(Reason.ShuffleFailure, accused, null, null, null, null);
    }

    // Sent in phase three if an output is missing.
    public static Blame MissingOutput(VerificationKey accused) {
        return new Blame(Reason.MissingOutput, accused, null, null, null, null);
    }

    // Sent when there is a failure in phase two and in the subsequent equivocation check.
    public static Blame ShuffleAndEquivocationFailure(
            DecryptionKey privateKey,
            Queue<Packet> packets
    ) {
        return new Blame(
                Reason.ShuffleAndEquivocationFailure, null, null, privateKey, null, packets);
    }
}
