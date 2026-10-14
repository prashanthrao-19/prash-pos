import { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { getAllBranchesByStore } from "@/Redux Toolkit/features/branch/branchThunks";
import { getOrdersByBranch } from "@/Redux Toolkit/features/order/orderThunks";

const formatSaleDate = (value) => {
  if (!value) return "Date unavailable";

  const saleDate = new Date(value);
  if (Number.isNaN(saleDate.getTime())) return "Date unavailable";

  const today = new Date();
  const yesterday = new Date();
  yesterday.setDate(today.getDate() - 1);

  const isSameDay = (left, right) =>
    left.getFullYear() === right.getFullYear() &&
    left.getMonth() === right.getMonth() &&
    left.getDate() === right.getDate();

  if (isSameDay(saleDate, today)) return "Today";
  if (isSameDay(saleDate, yesterday)) return "Yesterday";

  return saleDate.toLocaleDateString(undefined, {
    month: "short",
    day: "numeric",
    year: saleDate.getFullYear() === today.getFullYear() ? undefined : "numeric",
  });
};

const RecentSales = () => {
  const dispatch = useDispatch();
  const { store } = useSelector((state) => state.store);
  const [recentSales, setRecentSales] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let isCurrentRequest = true;

    const loadRecentSales = async () => {
      if (!store?.id) {
        setRecentSales([]);
        setLoading(true);
        return;
      }

      setLoading(true);
      setError(null);

      try {
        const branches = await dispatch(
          getAllBranchesByStore({
            storeId: store.id,
            jwt: localStorage.getItem("jwt"),
          })
        ).unwrap();

        const ordersByBranch = await Promise.all(
          branches.map(async (branch) => {
            const orders = await dispatch(
              getOrdersByBranch({ branchId: branch.id })
            ).unwrap();

            return orders.map((order) => ({
              ...order,
              branchName: branch.name,
            }));
          })
        );

        const sales = ordersByBranch
          .flat()
          .sort((left, right) => new Date(right.createdAt) - new Date(left.createdAt))
          .slice(0, 5);

        if (isCurrentRequest) setRecentSales(sales);
      } catch (requestError) {
        if (isCurrentRequest) {
          setRecentSales([]);
          setError(
            typeof requestError === "string"
              ? requestError
              : "Unable to load recent sales."
          );
        }
      } finally {
        if (isCurrentRequest) setLoading(false);
      }
    };

    loadRecentSales();

    return () => {
      isCurrentRequest = false;
    };
  }, [dispatch, store?.id]);

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-xl font-semibold">Recent Sales</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="space-y-4">
          {loading ? (
            <p className="text-sm text-gray-500">Loading recent sales...</p>
          ) : error ? (
            <p className="text-sm text-red-600">{error}</p>
          ) : recentSales.length === 0 ? (
            <p className="text-sm text-gray-500">No sales yet.</p>
          ) : (
            recentSales.map((sale) => (
              <div
                key={sale.id}
                className="flex items-center justify-between border-b pb-3 last:border-0 last:pb-0"
              >
                <div>
                  <p className="font-medium">{sale.branchName}</p>
                  <p className="text-sm text-gray-500">
                    {formatSaleDate(sale.createdAt)}
                  </p>
                </div>
                <p className="font-semibold">
                  ₹{Number(sale.totalAmount || 0).toLocaleString("en-IN")}
                </p>
              </div>
            ))
          )}
        </div>
      </CardContent>
    </Card>
  );
};

export default RecentSales;
