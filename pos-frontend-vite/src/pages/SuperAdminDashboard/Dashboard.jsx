import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";
import { Store, Clock, TrendingUp, AlertTriangle } from "lucide-react";
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, PieChart, Pie, Cell } from "recharts";
import {
  getDashboardSummary,
  getStoreRegistrationStats,
  getStoreStatusDistribution
} from "../../Redux Toolkit/features/adminDashboard/adminDashboardThunks";
import { getAllStores } from "../../Redux Toolkit/features/store/storeThunks";

const COLORS = ["#10b981", "#f59e0b", "#ef4444"];

const StatCard = ({ title, value, icon, description, trend }) => (
  <Card>
    <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
      <CardTitle className="text-sm font-medium text-muted-foreground">
        {title}
      </CardTitle>
      {icon}
    </CardHeader>
    <CardContent>
      <div className="text-2xl font-bold">{value}</div>
      <p className="text-xs text-muted-foreground flex items-center gap-1">
        {trend !== undefined && (
          <span className={trend > 0 ? "text-green-600" : trend < 0 ? "text-red-600" : ""}>
            {trend > 0 ? "+" : ""}{trend}%
          </span>
        )}
        {description}
      </p>
    </CardContent>
  </Card>
);

export default function Dashboard() {
  const dispatch = useDispatch();
  const { stores } = useSelector((state) => state.store);
  const [activityLoading, setActivityLoading] = useState(true);
  const [activityError, setActivityError] = useState(null);
  const {
    dashboardSummary,
    storeRegistrationStats,
    storeStatusDistribution,
    loading,
    error
  } = useSelector((state) => state.adminDashboard);

  useEffect(() => {
    dispatch(getDashboardSummary());
    dispatch(getStoreRegistrationStats());
    dispatch(getStoreStatusDistribution());

    let isCurrentRequest = true;
    dispatch(getAllStores())
      .unwrap()
      .catch(() => {
        if (isCurrentRequest) {
          setActivityError("Unable to load recent store registrations.");
        }
      })
      .finally(() => {
        if (isCurrentRequest) setActivityLoading(false);
      });

    return () => {
      isCurrentRequest = false;
    };
  }, [dispatch]);

  const recentRegistrations = [...(stores || [])]
    .sort((left, right) => new Date(right.createdAt) - new Date(left.createdAt))
    .slice(0, 3);

  const formatRegistrationDate = (value) => {
    if (!value) return "Registration date unavailable";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return "Registration date unavailable";
    return `Registered ${date.toLocaleDateString(undefined, {
      month: "short",
      day: "numeric",
      year: "numeric",
    })}`;
  };

  const getStoreStatusLabel = (status) => {
    const normalizedStatus = status?.toUpperCase();
    if (normalizedStatus === "ACTIVE") return "Active";
    if (normalizedStatus === "PENDING") return "Pending approval";
    if (normalizedStatus === "BLOCKED") return "Blocked";
    return "Status unavailable";
  };

  const getStoreStatusColor = (status) => {
    const normalizedStatus = status?.toUpperCase();
    if (normalizedStatus === "PENDING") return "bg-yellow-500";
    if (normalizedStatus === "BLOCKED") return "bg-red-500";
    return "bg-green-500";
  };

  // Prepare data for charts
  const barData = storeRegistrationStats?.map((item) => ({
    date: item.date || item.day || item.label,
    stores: item.count || item.value || 0
  })) || [];

  const pieData = storeStatusDistribution
    ? [
        { name: "Active", value: storeStatusDistribution.active, color: COLORS[0] },
        { name: "Pending", value: storeStatusDistribution.pending, color: COLORS[1] },
        { name: "Blocked", value: storeStatusDistribution.blocked, color: COLORS[2] },
      ]
    : [];

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-3xl font-bold tracking-tight">Dashboard</h2>
        <p className="text-muted-foreground">
          Overview of all stores and system statistics
        </p>
      </div>

      {loading && <div className="text-center py-8">Loading dashboard...</div>}
      {error && <div className="text-center py-8 text-red-500">{error}</div>}

      {/* Stat Cards */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        <StatCard
          title="Total Stores"
          value={dashboardSummary?.totalStores ?? "-"}
          icon={<Store className="h-4 w-4 text-muted-foreground" />}
          description="from last month"
          trend={undefined}
        />
        <StatCard
          title="Active Stores"
          value={dashboardSummary?.activeStores ?? "-"}
          icon={<TrendingUp className="h-4 w-4 text-muted-foreground" />}
          description="currently operational"
          trend={undefined}
        />
        <StatCard
          title="Blocked Stores"
          value={dashboardSummary?.blockedStores ?? "-"}
          icon={<AlertTriangle className="h-4 w-4 text-muted-foreground" />}
          description="suspended accounts"
          trend={undefined}
        />
        <StatCard
          title="Pending Requests"
          value={dashboardSummary?.pendingStores ?? "-"}
          icon={<Clock className="h-4 w-4 text-muted-foreground" />}
          description="awaiting approval"
          trend={undefined}
        />
      </div>

      {/* Charts */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-7">
        <Card className="col-span-4">
          <CardHeader>
            <CardTitle>Store Registrations (Last 7 Days)</CardTitle>
          </CardHeader>
          <CardContent className="pl-2">
            <ResponsiveContainer width="100%" height={350}>
              <BarChart data={barData}>
                <XAxis
                  dataKey="date"
                  stroke="#888888"
                  fontSize={12}
                  tickLine={false}
                  axisLine={false}
                />
                <YAxis
                  stroke="#888888"
                  fontSize={12}
                  tickLine={false}
                  axisLine={false}
                  tickFormatter={(value) => `${value}`}
                />
                <CartesianGrid strokeDasharray="3 3" className="stroke-muted" />
                <Tooltip />
                <Bar
                  dataKey="stores"
                  fill="currentColor"
                  radius={[4, 4, 0, 0]}
                  className="fill-primary"
                />
              </BarChart>
            </ResponsiveContainer>
          </CardContent>
        </Card>

        <Card className="col-span-3">
          <CardHeader>
            <CardTitle>Store Status Distribution</CardTitle>
          </CardHeader>
          <CardContent>
            <ResponsiveContainer width="100%" height={350}>
              <PieChart>
                <Pie
                  data={pieData}
                  cx="50%"
                  cy="50%"
                  labelLine={false}
                  label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
                  outerRadius={80}
                  fill="#8884d8"
                  dataKey="value"
                >
                  {pieData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.color} />
                  ))}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          </CardContent>
        </Card>
      </div>

      {/* Recent registrations from live store records */}
      <Card>
        <CardHeader>
          <CardTitle>Recent Activity</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {activityLoading ? (
              <p className="text-sm text-muted-foreground">Loading recent registrations...</p>
            ) : activityError ? (
              <p className="text-sm text-red-500">{activityError}</p>
            ) : recentRegistrations.length === 0 ? (
              <p className="text-sm text-muted-foreground">No stores have been registered yet.</p>
            ) : (
              recentRegistrations.map((store) => (
                <div key={store.id} className="flex items-center gap-4 p-4 border rounded-lg">
                  <div className={`w-2 h-2 rounded-full ${getStoreStatusColor(store.status)}`}></div>
                  <div className="flex-1">
                    <p className="text-sm font-medium">
                      Store "{store.brand || "Unnamed store"}" registered
                    </p>
                    <p className="text-xs text-muted-foreground">
                      {formatRegistrationDate(store.createdAt)} · {getStoreStatusLabel(store.status)}
                    </p>
                  </div>
                </div>
              ))
            )}
          </div>
        </CardContent>
      </Card>
    </div>
  );
} 
