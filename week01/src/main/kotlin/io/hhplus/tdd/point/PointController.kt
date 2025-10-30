package io.hhplus.tdd.point

import io.hhplus.tdd.common.response.ApiResponse
import io.hhplus.tdd.point.service.PointChargeService
import io.hhplus.tdd.point.service.PointQueryService
import io.hhplus.tdd.point.service.PointUseService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/point")
class PointController(
    private val pointQueryService: PointQueryService,
    private val pointChargeService: PointChargeService,
    private val pointUseService: PointUseService
) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    /**
     * 특정 유저의 포인트를 조회하는 기능
     *
     * @param id 사용자 ID
     * @return 사용자의 포인트 정보를 포함한 API 응답
     */
    @GetMapping("{id}")
    fun point(
        @PathVariable id: Long,
    ): ApiResponse<UserPoint> {
        logger.info("포인트 조회 API 요청: userId=$id")
        val result = pointQueryService.getPoint(id)
        return ApiResponse.success(result)
    }

    /**
     * 특정 유저의 포인트 충전/이용 내역을 조회하는 기능
     *
     * @param id 사용자 ID
     * @return 사용자의 포인트 충전/사용 내역 목록을 포함한 API 응답 (최신순, 최대 100건)
     */
    @GetMapping("{id}/histories")
    fun history(
        @PathVariable id: Long,
    ): ApiResponse<List<PointHistory>> {
        logger.info("포인트 내역 조회 API 요청: userId=$id")
        val result = pointQueryService.getHistories(id)
        return ApiResponse.success(result)
    }

    /**
     * 특정 유저의 포인트를 충전하는 기능
     *
     * @param id 사용자 ID
     * @param amount 충전 금액
     * @return 충전 후 사용자의 포인트 정보를 포함한 API 응답
     */
    @PatchMapping("{id}/charge")
    fun charge(
        @PathVariable id: Long,
        @RequestBody amount: Long,
    ): ApiResponse<UserPoint> {
        logger.info("포인트 충전 API 요청: userId=$id, amount=$amount")
        val result = pointChargeService.charge(id, amount)
        return ApiResponse.success(result)
    }

    /**
     * 특정 유저의 포인트를 사용하는 기능
     *
     * @param id 사용자 ID
     * @param amount 사용 금액
     * @return 사용 후 사용자의 포인트 정보를 포함한 API 응답
     */
    @PatchMapping("{id}/use")
    fun use(
        @PathVariable id: Long,
        @RequestBody amount: Long,
    ): ApiResponse<UserPoint> {
        logger.info("포인트 사용 API 요청: userId=$id, amount=$amount")
        val result = pointUseService.use(id, amount)
        return ApiResponse.success(result)
    }
}