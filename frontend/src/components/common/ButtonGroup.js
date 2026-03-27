"use client";

import { testAdminScope, testUserScope, testUserScopeToAdmin } from "@/utils/api";

export default function ButtonGroup() {
    return (
        <div className="button-group">
            <button onClick={testAdminScope} className="action-btn">
                관리자 SCOPE
            </button>
            <button onClick={testUserScope} className="action-btn">
                일반사용자 SCOPE
            </button>
            <button onClick={testUserScopeToAdmin} className="action-btn">
                일반사용자로 관리자 접근
            </button>
        </div>
    );
}
