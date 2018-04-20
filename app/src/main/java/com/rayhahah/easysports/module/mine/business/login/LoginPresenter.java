package com.rayhahah.easysports.module.mine.business.login;

import com.rayhahah.easysports.app.C;
import com.rayhahah.easysports.app.MyApp;
import com.rayhahah.easysports.bean.db.LocalUser;
import com.rayhahah.easysports.module.mine.api.MineApiFactory;
import com.rayhahah.easysports.module.mine.bean.ESUser;
import com.rayhahah.greendao.gen.LocalUserDao;
import com.rayhahah.rbase.base.RBasePresenter;
import com.rayhahah.rbase.utils.useful.RxSchedulers;
import com.rayhahah.rbase.utils.useful.SPManager;

import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;

/**
 * ┌───┐ ┌───┬───┬───┬───┐ ┌───┬───┬───┬───┐ ┌───┬───┬───┬───┐ ┌───┬───┬───┐
 * │Esc│ │ F1│ F2│ F3│ F4│ │ F5│ F6│ F7│ F8│ │ F9│F10│F11│F12│ │P/S│S L│P/B│ ┌┐    ┌┐    ┌┐
 * └───┘ └───┴───┴───┴───┘ └───┴───┴───┴───┘ └───┴───┴───┴───┘ └───┴───┴───┘ └┘    └┘    └┘
 * ┌──┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───────┐┌───┬───┬───┐┌───┬───┬───┬───┐
 * │~`│! 1│@ 2│# 3│$ 4│% 5│^ 6│& 7│* 8│( 9│) 0│_ -│+ =│ BacSp ││Ins│Hom│PUp││N L│ / │ * │ - │
 * ├──┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─────┤├───┼───┼───┤├───┼───┼───┼───┤
 * │Tab │ Q │ W │ E │ R │ T │ Y │ U │ I │ O │ P │{ [│} ]│ | \ ││Del│End│PDn││ 7 │ 8 │ 9 │   │
 * ├────┴┬──┴┬──┴┬──┴┬──┴┬──┴┬──┴┬──┴┬──┴┬──┴┬──┴┬──┴┬──┴─────┤└───┴───┴───┘├───┼───┼───┤ + │
 * │Caps │ A │ S │ D │ F │ G │ H │ J │ K │ L │: ;│" '│ Enter  │             │ 4 │ 5 │ 6 │   │
 * ├─────┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴────────┤    ┌───┐    ├───┼───┼───┼───┤
 * │Shift  │ Z │ X │ C │ V │ B │ N │ M │< ,│> .│? /│  Shift   │    │ ↑ │    │ 1 │ 2 │ 3 │   │
 * ├────┬──┴─┬─┴──┬┴───┴───┴───┴───┴───┴──┬┴───┼───┴┬────┬────┤┌───┼───┼───┐├───┴───┼───┤ E││
 * │Ctrl│Ray │Alt │         Space         │ Alt│code│fuck│Ctrl││ ← │ ↓ │ → ││   0   │ . │←─┘│
 * └────┴────┴────┴───────────────────────┴────┴────┴────┴────┘└───┴───┴───┘└───────┴───┴───┘
 *
 * @author Rayhahah
 * @time 2017/7/21
 * @tips 这个类是Object的子类
 * @fuction
 */
public class LoginPresenter extends RBasePresenter<LoginContract.ILoginView>
        implements LoginContract.ILoginPresenter {
    public LoginPresenter(LoginContract.ILoginView view) {
        super(view);
    }

    @Override
    public void login(final String username, String password) {
        addSubscription(MineApiFactory.getUserInfo(username, password).subscribe(new Consumer<ESUser>() {
            @Override
            public void accept(@NonNull ESUser esUser) throws Exception {
                if (esUser.getStatus() != C.RESPONSE_SUCCESS) {
                    mView.loginFailed();
                    return;
                }
                Observable.just(esUser).map(new Function<ESUser, Long>() {
                    @Override
                    public Long apply(@NonNull ESUser user) throws Exception {
                        LocalUserDao localUserDao = MyApp.getDaoSession().getLocalUserDao();
                        LocalUser localUser = new LocalUser();
                        ESUser.DataBean userData = user.getData();
                        localUser.setEssysport_id(userData.getId() + "");
                        localUser.setUser_name(userData.getUsername());
                        localUser.setPassword(userData.getPassword());
                        localUser.setScreen_name(userData.getScreenname());
                        localUser.setQuestion(userData.getQuestion());
                        localUser.setAnswer(userData.getAnswer());
                        localUser.setTel(userData.getPhone());
                        localUser.setEmail(userData.getEmail());
                        localUser.setCover(userData.getCover());
                        localUser.setIs_day_theme(SPManager.get().getStringValue(C.SP.THEME));
                        localUser.setHupu_user_name(userData.getHupuUsername());
                        localUser.setHupu_password(userData.getHupuPassword());
                        long rowId = localUserDao.insertOrReplace(localUser);
                        if (rowId > 0) {
                            SPManager.get().putString(C.SP.CURRENT_USER, userData.getUsername());
                            SPManager.get().putString(C.SP.IS_LOGIN, C.TRUE);
                            MyApp.setCurrentUser(localUser);
                        }
                        return rowId;
                    }
                }).compose(RxSchedulers.<Long>ioMain()).subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(@NonNull Long rowId) throws Exception {
                        if (rowId != null) {
                            mView.loginSuccess();
                        } else {
                            mView.loginFailed();
                        }
                    }
                });
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(@NonNull Throwable throwable) throws Exception {
                mView.loginFailed();
            }
        }));
    }
}
