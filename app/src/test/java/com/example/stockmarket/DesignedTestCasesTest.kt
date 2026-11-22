package com.example.stockmarket


import com.example.stockmarket.data.model.Company
import com.example.stockmarket.data.model.Transaction
import com.example.stockmarket.data.model.TxType
import com.example.stockmarket.data.repository.OrderSide
import com.example.stockmarket.data.repository.RegistrationDraft

import com.example.stockmarket.data.repository.validatePassword
import com.example.stockmarket.feature.auth.LoginUiState
import com.example.stockmarket.feature.auth.SignUpUiState
import com.example.stockmarket.feature.registration.canContinue
import com.example.stockmarket.feature.trade.OrderUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import com.example.stockmarket.data.repository.OrderRules


@Suppress("FunctionName")
@OptIn(ExperimentalCoroutinesApi::class)
class DesignedTestCasesTest {

    // TradingStore uses the main thread internally, so the test supplies a stand-in
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val nbm = Company(
        symbol = "NBM",
        name = "National Bank of Malawi",
        sector = "Banking",
        price = 2010.0,
        changePct = 1.25
    )

    private val tnm = Company(
        symbol = "TNM",
        name = "Telekom Networks Malawi",
        sector = "Telecommunications",
        price = 24.50,
        changePct = -0.80
    )

    @Test
    fun AUTH_SIGNUP_001_validDetails_formIsValid() {
        val state = SignUpUiState(
            fullName = "Test User", identifier = "test@mse.test",
            password = "Sample123", confirm = "Sample123"
        )
        assertTrue(state.isValid)
    }

    @Test
    fun AUTH_SIGNUP_002_emptyFields_showThreeMessages() {
        val state = SignUpUiState(showErrors = true)

        assertEquals("Enter your full name", state.nameError)
        assertEquals("Enter a valid email or phone number", state.identifierError)
        assertEquals("Use at least 8 characters", state.passwordError)
        assertFalse(state.isValid)
    }

    @Test
    fun AUTH_SIGNUP_003_invalidEmail_isRejected() {
        val state = SignUpUiState(
            fullName = "Test User", identifier = "abc",
            password = "Sample123", confirm = "Sample123", showErrors = true
        )
        assertEquals("Enter a valid email or phone number", state.identifierError)
    }

    @Test
    fun AUTH_SIGNUP_004_passwordWithoutNumber_isRejected() {
        assertEquals("Include at least one letter and one number", validatePassword("abcdefgh"))
    }

    @Test
    fun AUTH_SIGNUP_005_sevenCharacterPassword_isRejected() {
        assertEquals("Use at least 8 characters", validatePassword("Abc1234"))
    }

    @Test
    fun AUTH_SIGNUP_006_eightCharacterPassword_isAccepted() {
        assertNull(validatePassword("Abc12345"))
    }

    @Test
    fun AUTH_SIGNUP_007_passwordsDoNotMatch_isRejected() {
        val state = SignUpUiState(
            fullName = "Test User", identifier = "mismatch@mse.test",
            password = "Sample123", confirm = "Sample124", showErrors = true
        )
        assertEquals("Passwords do not match", state.confirmError)
        assertFalse(state.isValid)
    }

    @Test
    fun AUTH_LOGIN_004_bothFieldsEmpty_cannotLogin() {
        assertFalse(LoginUiState().canLogin)
    }

    @Test
    fun AUTH_LOGIN_005_emailOnly_cannotLogin() {
        assertFalse(LoginUiState(identifier = "demo@mse.test").canLogin)
    }

    // ---------------- Investor registration ----------------

    @Test
    fun REG_WIZARD_002_onlyNameEntered_cannotContinue() {
        assertFalse(canContinue(RegistrationDraft(step = 0, fullName = "Test User")))
    }

    @Test
    fun REG_WIZARD_003_idNumberOfThreeCharacters_cannotContinue() {
        assertFalse(canContinue(RegistrationDraft(step = 1, idNumber = "123", idPhotoTaken = true)))
    }

    @Test
    fun REG_WIZARD_004_idNumberOfFourCharacters_canContinue() {
        assertTrue(canContinue(RegistrationDraft(step = 1, idNumber = "1234", idPhotoTaken = true)))
    }

    @Test
    fun REG_WIZARD_007_riskNotAccepted_cannotSubmit() {
        assertFalse(canContinue(RegistrationDraft(step = 4, riskAccepted = false)))
    }

    // ---------------- Trading ----------------

    @Test
    fun TRD_BUY_006_121Shares_isAllowed() {
        assertNull(OrderRules.validateOrder(OrderSide.BUY, 121, 2010.0, cash = 250_000.0, owned = 300))
    }

    @Test
    fun TRD_BUY_007_122Shares_isRefused() {
        assertEquals(
            "Not enough cash. You need MK 250,124.40 but have MK 250,000.00",
            OrderRules.validateOrder(OrderSide.BUY, 121, 2010.0, cash = 250_000.0, owned = 300)
        )
    }

    @Test
    fun TRD_SELL_001_fiveShares_youReceiveSubtotalMinusFees() {
        val state = OrderUiState(company = nbm, side = OrderSide.SELL, quantityText = "5")

        assertEquals(10_050.0, state.subtotal, 0.001)
        assertEquals(201.0, state.fees, 0.001)
        assertEquals(9_849.0, state.total, 0.001)
    }

    @Test
    fun TRD_SELL_002_301SharesWhen300Owned_isRefused() {
        assertEquals("You only own 300 shares", OrderRules.validateOrder(OrderSide.SELL, 301, 2010.0, cash = 250_000.0, owned = 300))
    }

    @Test
    fun TRD_SELL_003_companyNotOwned_isRefused() {
        assertEquals("You only own 0 shares", OrderRules.validateOrder(OrderSide.SELL, 1, 3040.0, cash = 250_000.0, owned = 0))
    }

    // ---------------- Wallet ----------------

    @Test
    fun WAL_DEPOSIT_002_500_isBelowTheMinimum() {
        assertEquals("The minimum is MK 1,000", OrderRules.validateDeposit(500.0))
    }

    @Test
    fun WAL_DEPOSIT_003_exactlyTheMinimum_isAccepted() {
        assertNull(OrderRules.validateDeposit(1_000.0))
    }

    @Test
    fun WAL_WITHDRAW_002_moreThanTheBalance_isRefused() {
        assertEquals("You only have MK 250,000.00 available", OrderRules.validateWithdrawal(250_001.0, cash = 250_000.0))
    }

    @Test
    fun WAL_WITHDRAW_003_exactlyTheBalance_isAccepted() {
        assertNull(OrderRules.validateWithdrawal(250_001.0, cash = 250_000.0))
    }

    // ---------------- History ----------------

    @Test
    fun HIS_DETAIL_001_boughtNbm_amountIs555000() {
        val tx = Transaction(2, TxType.BUY, "NBM", 300, 1850.0, "05 Oct 2026")
        assertEquals(555_000.0, tx.amount, 0.001)
    }

    @Test
    fun HIS_DETAIL_002_deposit_isCashWithTheDepositedAmount() {
        val tx = Transaction(1, TxType.DEPOSIT, null, 0, 1_000_000.0, "02 Oct 2026")

        assertTrue(tx.isCash)
        assertEquals(1_000_000.0, tx.amount, 0.001)
    }

    // ---------------- Accuracy of financial data ----------------

    @Test
    fun ACC_ORDER_001_tenTnmShares_totalsAreCorrect() {
        val state = OrderUiState(company = tnm, side = OrderSide.BUY, quantityText = "10")

        assertEquals(245.0, state.subtotal, 0.001)
        assertEquals(4.90, state.fees, 0.001)
        assertEquals(249.90, state.total, 0.001)
    }

    @Test
    fun ACC_ORDER_002_sevenTnmShares_feesAreRoundedTo2Decimals() {
        val state = OrderUiState(company = tnm, side = OrderSide.BUY, quantityText = "7")

        assertEquals(171.50, state.subtotal, 0.001)
        assertEquals(3.43, state.fees, 0.001)
        assertEquals(174.93, state.total, 0.001)
    }

    @Test
    fun ACC_ORDER_003_emptyQuantity_totalsAreZero() {
        val state = OrderUiState(company = nbm, side = OrderSide.BUY, quantityText = "")

        assertEquals(0.0, state.subtotal, 0.001)
        assertEquals(0.0, state.fees, 0.001)
        assertEquals(0.0, state.total, 0.001)
        assertFalse(state.canPlace)
    }
}