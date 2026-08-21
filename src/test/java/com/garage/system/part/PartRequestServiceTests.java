package com.garage.system.part;

import com.garage.system.mechanic.MechanicRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartRequestServiceTests {
    @Mock PartRequestRepository requestRepository;
    @Mock PartRepository partRepository;
    @Mock MechanicRepository mechanicRepository;
    @InjectMocks PartRequestService service;

    @Test
    void approvalAddsRequestedQuantityToStockAndClosesRequest() {
        var part = part(4L, 7);
        var request = request(10L, part, 5, PartRequestStatus.PENDING);
        when(requestRepository.findLockedById(10L)).thenReturn(Optional.of(request));
        when(partRepository.findLockedById(4L)).thenReturn(Optional.of(part));

        var result = service.decide(10L, PartRequestStatus.APPROVED);

        assertEquals(12, part.getStockQty());
        assertEquals(PartRequestStatus.APPROVED, result.getStatus());
        assertNotNull(result.getDecidedAt());
    }

    @Test
    void decidedRequestCannotChangeOrIncreaseStockAgain() {
        var part = part(4L, 7);
        var request = request(10L, part, 5, PartRequestStatus.APPROVED);
        when(requestRepository.findLockedById(10L)).thenReturn(Optional.of(request));

        assertThrows(ResponseStatusException.class,
                () -> service.decide(10L, PartRequestStatus.REJECTED));
        assertEquals(7, part.getStockQty());
        verify(partRepository, never()).findLockedById(4L);
    }

    private Part part(Long id, int stock) {
        var part = new Part();
        ReflectionTestUtils.setField(part, "id", id);
        part.setStockQty(stock);
        return part;
    }

    private PartRequest request(Long id, Part part, int quantity, PartRequestStatus status) {
        var request = new PartRequest();
        ReflectionTestUtils.setField(request, "id", id);
        request.setPart(part);
        request.setQuantity(quantity);
        request.setStatus(status);
        return request;
    }
}
