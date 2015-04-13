package com.ksy.media.player.util;

import java.io.Serializable;

public interface DRMRetrieverResponseHandler extends Serializable {

	void onSuccess(int paramInt, String cek);

	void onFailure(int paramInt, String response, Throwable paramThrowable);
}
