"use client";

/**
 * 로그인 직후, 비로그인 상태에서 이 브라우저에 저장해둔 설정과
 * 계정에 저장된 설정이 서로 다를 때 어느 쪽을 쓸지 묻는 확인창.
 *
 * 리그 순서는 두 순서를 합칠 방법이 없어 택일이 불가피하고,
 * 즐겨찾기는 합집합이 되므로 "합치기"가 기본 동작이다.
 */
const MergePreferencesModal = ({ isOpen, onMerge, onKeepAccount, busy }) => {
    if (!isOpen) return null;

    return (
        <>
            <div className="modal-overlay"></div>
            <div className="league-order-modal merge-prefs-modal">
                <div className="modal-header">
                    <h3>설정을 합칠까요?</h3>
                </div>

                <div className="modal-body">
                    <p>
                        로그인 전에 이 브라우저에 저장해둔 즐겨찾기와 리그 순서가 있습니다.
                        계정에 저장된 설정과 달라요.
                    </p>
                    <p className="merge-prefs-note">
                        합치면 즐겨찾기는 양쪽을 모두 남기고, 리그 순서는 이 브라우저의 순서를 씁니다.
                    </p>
                </div>

                <div className="modal-footer">
                    <button onClick={onKeepAccount} className="cancel-btn" disabled={busy}>
                        계정 설정 사용
                    </button>
                    <button onClick={onMerge} className="save-btn" disabled={busy}>
                        {busy ? '합치는 중...' : '합치기'}
                    </button>
                </div>
            </div>
        </>
    );
};

export default MergePreferencesModal;
