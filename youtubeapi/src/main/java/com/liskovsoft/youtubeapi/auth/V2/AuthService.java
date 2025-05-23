package com.liskovsoft.youtubeapi.auth.V2;

import androidx.annotation.Nullable;

import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.youtubeapi.app.AppService;
import com.liskovsoft.youtubeapi.auth.models.auth.AccessToken;
import com.liskovsoft.youtubeapi.auth.models.auth.RefreshToken;
import com.liskovsoft.youtubeapi.auth.models.auth.UserCode;
import com.liskovsoft.youtubeapi.auth.models.info.AccountInt;
import com.liskovsoft.youtubeapi.auth.models.info.AccountsList;
import com.liskovsoft.youtubeapi.common.helpers.RetrofitHelper;
import retrofit2.Call;

import java.util.List;

public class AuthService {
    private static final String TAG = AuthService.class.getSimpleName();
    private static AuthService sInstance;
    private final AuthApi mAuthApi;
    private static final int REFRESH_TOKEN_ATTEMPTS = 200;
    private static final long REFRESH_TOKEN_ATTEMPT_INTERVAL_MS = 3_000;
    private final AppService mAppService;

    private AuthService() {
        mAuthApi = RetrofitHelper.create(AuthApi.class);
        mAppService = AppService.instance();
    }

    public static AuthService instance() {
        if (sInstance == null) {
            sInstance = new AuthService();
        }

        return sInstance;
    }

    /**
     * Returns user code that user should apply on the page<br/>
     * <a href=https://youtube.com/activate>https://youtube.com/activate</a>
     * @return response with user code and device code
     */
    public UserCode getUserCode() {
        Call<UserCode> wrapper = mAuthApi.getUserCode(
                AuthApiHelper.getUserCodeQuery(mAppService.getClientId())
        );
        return RetrofitHelper.get(wrapper);
    }

    /**
     * Note, before calling this method user should apply the 'user code' on the page<br/>
     * <a href=https://youtube.com/activate>https://youtube.com/activate</a>
     * @param deviceCode the code contained inside the response of the method {@link #getUserCode()}
     * @return refresh token that should be stored inside the app registry for future use
     */
    public RefreshToken getRefreshToken(String deviceCode) {
        Call<RefreshToken> wrapper = mAuthApi.getRefreshToken(
                AuthApiHelper.getRefreshTokenQuery(
                        deviceCode,
                        mAppService.getClientId(),
                        mAppService.getClientSecret())
        );
        return RetrofitHelper.get(wrapper);
    }

    /**
     * Returns temporal access token that should be refreshed after some period of time
     * @param refreshToken token obtained from previous method
     * @return temporal access token
     */
    public AccessToken getAccessToken(String refreshToken) {
        Call<AccessToken> wrapper = mAuthApi.getAccessToken(
                AuthApiHelper.getAccessTokenQuery(refreshToken,
                        mAppService.getClientId(),
                        mAppService.getClientSecret())
        );
        return RetrofitHelper.getWithErrors(wrapper);
    }

    public AccessToken getAccessTokenRaw(String rawJsonAuthData) {
        Call<AccessToken> wrapper = mAuthApi.getAccessToken(rawJsonAuthData);
        return RetrofitHelper.get(wrapper);
    }

    public RefreshToken getRefreshTokenWait(String deviceCode) throws InterruptedException {
        RefreshToken tokenResult = null;

        for (int i = 0; i < REFRESH_TOKEN_ATTEMPTS; i++) {
            Thread.sleep(REFRESH_TOKEN_ATTEMPT_INTERVAL_MS);

            tokenResult = getRefreshToken(deviceCode);

            if (tokenResult != null && tokenResult.getRefreshToken() != null) {
                break;
            }
        }

        if (tokenResult != null && tokenResult.getRefreshToken() != null) {
            return tokenResult;
        } else {
            String msg = String.format("Error. Refresh token is empty!\nDebug data: device code: %s, client id: %s, client secret: %s\nError msg: %s",
                    deviceCode,
                    mAppService.getClientId(),
                    mAppService.getClientSecret(),
                    tokenResult != null ? tokenResult.getError() : "");

            Log.e(TAG, msg);
            throw new IllegalStateException(msg);
        }
    }

    @Nullable
    public List<AccountInt> getAccounts() {
        Call<AccountsList> wrapper = mAuthApi.getAccountsList(AuthApiHelper.getAccountsListQuery());

        AccountsList accountsList = RetrofitHelper.get(wrapper);

        return accountsList != null ? accountsList.getAccounts() : null;
    }
}
