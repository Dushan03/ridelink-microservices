package com.ridelink.fare.service;

import com.ridelink.fare.dto.ReceiptResponse;
import com.ridelink.fare.entity.Receipt;
import com.ridelink.fare.exception.ForbiddenOperationException;
import com.ridelink.fare.exception.ResourceNotFoundException;
import com.ridelink.fare.repository.ReceiptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing digital receipt lookups and ownership authorization.
 */
@Service
@Transactional(readOnly = true)
public class ReceiptService {

    private final ReceiptRepository receiptRepository;

    public ReceiptService(ReceiptRepository receiptRepository) {
        this.receiptRepository = receiptRepository;
    }

    /**
     * Retrieves a digital receipt by receipt ID with ownership validation.
     */
    public ReceiptResponse getReceiptById(String receiptId, String authenticatedAccountId, String authenticatedRole) {
        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt", receiptId));

        validateReceiptOwnership(receipt, authenticatedAccountId, authenticatedRole);
        return ReceiptResponse.from(receipt);
    }

    /**
     * Retrieves a digital receipt by payment ID with ownership validation.
     */
    public ReceiptResponse getReceiptByPaymentId(String paymentId, String authenticatedAccountId, String authenticatedRole) {
        Receipt receipt = receiptRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt for payment", paymentId));

        validateReceiptOwnership(receipt, authenticatedAccountId, authenticatedRole);
        return ReceiptResponse.from(receipt);
    }

    private void validateReceiptOwnership(Receipt receipt, String authenticatedAccountId, String authenticatedRole) {
        if ("ADMIN".equalsIgnoreCase(authenticatedRole)) {
            return;
        }
        if (receipt.getPassengerId() != null && !receipt.getPassengerId().equals(authenticatedAccountId)) {
            throw new ForbiddenOperationException("Access denied: You do not have permission to view this receipt");
        }
    }
}
