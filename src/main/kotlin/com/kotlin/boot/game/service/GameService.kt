package com.kotlin.boot.game.service

import com.kotlin.boot.event.CountPlus
import com.kotlin.boot.game.controller.dto.GameInfo
import com.kotlin.boot.game.controller.dto.JoinGameDto
import com.kotlin.boot.game.domain.GameEntity
import com.kotlin.boot.game.domain.GameResultEntity
import com.kotlin.boot.game.repository.infra.GameRepository
import com.kotlin.boot.game.repository.infra.GameResultLockRepository
import com.kotlin.boot.game.repository.infra.GameResultRepository
import com.kotlin.boot.global.dto.BaseResponse
import com.kotlin.boot.global.dto.GAME_BALL
import com.kotlin.boot.global.exception.BadRequestException
import com.kotlin.boot.global.exception.ErrorReason
import com.kotlin.boot.global.utils.getAutoNumber
import com.kotlin.boot.global.utils.randomUtils
import com.kotlin.boot.user.infra.repository.PlayGameUserRepository
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GameService(
    private val gameRepository: GameRepository,
    private val gameUserRepository: PlayGameUserRepository,
    private val gameResultRepository: GameResultRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val gameResultLockRepository: GameResultLockRepository
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    @Transactional(readOnly = true)
    fun gerParticipateGameInfos(round: Long?, phoneNumber: String): List<GameEntity> {
        gameUserRepository.findByPhoneNumber(phoneNumber)?.let {
            return gameRepository.findByUserIdAndPlayRound(
                it.userId,
                round ?: (gameResultRepository.findByStatus().id!!)
            )
        } ?: throw BadRequestException(ErrorReason.USER_INFO_NOT_FOUND, "휴대전화번호를 확인 하세요.")
    }

    @Transactional
    fun createGameRound() {
        gameResultRepository.findByStatusAndNormalNumberIsNotNull()?.let {
            throw BadRequestException(ErrorReason.ACTIVE_GAME_IS_EXIST, "ACTIVE GAME IS EXIST")
        } ?: gameResultRepository.save(GameResultEntity.ofAutoStart())
    }

    @Transactional
    fun participateInGame(joinGameDto: JoinGameDto): BaseResponse {
        val entryNumberList = joinGameDto.numbers
        val numberList = when {
            entryNumberList.isNullOrEmpty() -> GAME_BALL.getAutoNumber()
            entryNumberList.size < 5 -> {
                checkInputNumbers(entryNumberList.toLongArray())
                getHalfAutoRandom(
                    (4 - entryNumberList.size).toLong(),
                    entryNumberList.toLongArray()
                )
            }
            else -> throw BadRequestException(
                ErrorReason.INVALID_INPUT_DATA,
                "### 번호는 4개만 입력해주세요"
            )
        }
        val sb = StringBuilder()
        numberList.sorted().forEach {
            if (it >= 46 || it < 1) {
                log.info("번호 : >>> $numberList")
                throw BadRequestException(
                    ErrorReason.INVALID_INPUT_DATA,
                    "### 번호 -> 최소값 : 1 , 최대값 : 45"
                )
            }
            sb.append("$it,")
        }

        val submitNumbers = sb.toString().removeSuffix(",")
        //회원 정보 조회
        gameUserRepository.findByPhoneNumber(joinGameDto.phoneNumber)?.let {
            val currentRoundInfo = gameResultLockRepository.findByStatus()
            gameRepository.save(
                GameEntity.of(
                    it,
                    submitNumbers,
                    currentRoundInfo.id!!
                )
            )
            eventPublisher.publishEvent(CountPlus(currentRoundInfo))
        } ?: throw BadRequestException(
            ErrorReason.USER_INFO_NOT_FOUND,
            "### 유저 정보를 찾을 수 없습니다. 해당 번호로 가입 먼저 진행하세요."
        )
        return BaseResponse.of(submitNumbers)
    }

    fun checkInputNumbers(numberList: LongArray) {
        for (target in numberList) {
            var count = 0
            numberList.forEach {
                if (target == it) count += 1
            }
            if (count > 1)
                throw BadRequestException(
                    ErrorReason.INVALID_INPUT_DATA,
                    "### 번호 중복 : $target"
                )
        }
    }

    fun getGameRoundInfos(round: Long): List<GameInfo>? {
        return gameRepository.findByPlayRound(round)?.map {
            GameInfo.of(it)
        }?.toList()
    }

    private fun getHalfAutoRandom(count: Long, numberList: LongArray): List<Long> {
        return count.randomUtils(numberList.toMutableList())
    }
}
