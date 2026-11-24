package io.github.debajdutta.orderservice.order.search;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The whole flow in three explicit steps:
 *   request (API terms) --map--> criteria (domain terms) --query--> entities --map--> response (API terms)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class OrderSearchService {

    private final OrderSearchRequestMapper requestMapper;
    private final OrderSearchQuery query;
    private final OrderSearchResponseMapper responseMapper;

    public OrderSearchResponse search(OrderSearchRequest request) {
        OrderSearchCriteria criteria = requestMapper.toCriteria(request);   // 1. take & map (400 if invalid)
        OrderSearchResult result = query.search(criteria);                  // 2. the slice's own query
        log.debug("Order search {} -> {} of {} matches", criteria, result.orders().size(), result.totalMatches());
        return responseMapper.toResponse(result, criteria);                 // 3. map to response
    }
}
