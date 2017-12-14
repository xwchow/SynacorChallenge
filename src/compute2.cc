#include <bits/stdc++.h>

using namespace std;
#define X first
#define Y second
typedef long long ll;
typedef pair<int, int> pii;
typedef vector<int> vi;
typedef vector<vi> vvi;

#define debug(x) cerr << #x << " = " << (x) << endl;
template<typename T>
ostream& operator<<(ostream& o, vector<T>& v) {
    for (auto& x : v) o << x << ' ';
    return o;
}

const int LIMIT = (1 << 15);
const int TARGET = 6;

int main(int argc, char *argv[]){
    std::ios_base::sync_with_stdio(false); cin.tie(0);

    for (int H = 0; H < LIMIT; H++) {
        int A = 4;
        int B = 1;
        stack<pii> stk;
        while (1) {
            if (A == 0) {
                A = (B+1) % LIMIT;
                if (stk.empty()) {
                    break;
                } else if (stk.top().X == 0) {
                    B = (B + stk.top().Y) % LIMIT;
                    stk.pop();
                    A = 0;
                } else if (stk.top().X == 1) {
                    B = (B + stk.top().Y * (H + 1)) % LIMIT;
                    stk.pop();
                    A = 0;
                } else {
                    B = A;
                    A = stk.top().X;
                    if (--stk.top().Y == 0) {
                        stk.pop();
                    }
                }
            } else {
                if (B) stk.emplace((A - 1 + LIMIT) % LIMIT, B);
                A = (A - 1 + LIMIT) % LIMIT;
                B = H;
            }
        }
        if (A == TARGET) cout << "MAGIC NUMBER = " << H << endl;
    }
}
